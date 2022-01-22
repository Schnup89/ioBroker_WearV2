package com.schnup.iobrokerw

import android.annotation.SuppressLint
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults

import androidx.compose.runtime.*
import androidx.wear.compose.material.ChipDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.focus.focusTarget

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

import com.schnup.iobrokerw.ui.theme.IoBrokerWTheme
import io.socket.client.Ack
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.wear.input.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Stable
data class dcIOChips(
    var nIndex: Int = -1,
    var sName: String = "N/A",
    var sVal: String = "N/A",
    var sStateID: String = "N/A",
    var sUnit: String = "null",
    var sIconB64: String = "null",
    var bWriteable: Boolean = false,
    var sColorOn: String = "null",
    var nMinMax: ClosedFloatingPointRange<Float> = 0.toFloat().rangeTo(20.toFloat()),
    var nType: Int = 0,                     //0 = Chip, 1 = ToggleChip, 2 = Slider  Unknown = Chip
    var bUpdateCompose: Boolean = false     //Used for Change value to trigger Compose Update
)

class MainActivity : ComponentActivity() {
    val sIndicator = MutableStateFlow("?")
    val sNotify = MutableStateFlow("")
    val sUrl = MutableStateFlow("")
    val lChips = mutableStateListOf<dcIOChips>()
    var bIsLoading = MutableStateFlow(true)
    lateinit var mSocket: Socket
    var bSliderCoolDown = false

    //Compose update triggers automaticly if a chip is added oder removed, but not on changes inside the chip (dcIOChip-class)
    fun fTriggerChipListComposeUpdate(nIndex: Int){
        //Flip boolean and write it with copy, with that we trigger an element update on composedlist
        val bFlipedBool: Boolean = !lChips[nIndex].bUpdateCompose
        lChips[nIndex] = lChips[nIndex].copy(bUpdateCompose = bFlipedBool)
    }



    @ExperimentalComposeUiApi
    @SuppressLint("StateFlowValueCalledInComposition")
    @ExperimentalWearMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            val csIndicator by sIndicator.collectAsState()
            val csNotify by sNotify.collectAsState()
            val csUrl by sUrl.collectAsState()
            val cbIsLoading by bIsLoading.collectAsState()
            val hHaptic = LocalHapticFeedback.current


            if (cbIsLoading) {
                //Leave Splashscreen until Data is Loaded
            } else {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    it.data?.let { data ->
                        val results: Bundle = RemoteInput.getResultsFromIntent(data)
                        val sInpUrl: CharSequence? = results.getCharSequence("url")
                        sUrl.value = sInpUrl.toString()
                        this.getPreferences(Context.MODE_PRIVATE).edit().putString("sUrl",sInpUrl.toString()).apply()
                        sNotify.value = ""  //Remove "Bitte Server angeben-Notify"
                        //Connect if no Items feteched an Connection state is not connected
                        if (lChips.lastIndex == -1) {
                            //Connect to IOBroker and Callback Handler && Subscribe to IDs to get the States
                            if (fSetupSocket()) fSetupList()
                        }
                    }
                }

                IoBrokerWTheme {
                    val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()

                    Scaffold(
                        modifier = Modifier
                            .background(Color.Black),
                        timeText = {
                            TimeText(
                                leadingCurvedContent = {
                                    Text(
                                        text = csIndicator, //✔
                                        fontWeight = FontWeight.Bold,
                                        style =
                                        when (csIndicator) {
                                            "?" -> TimeTextDefaults.timeTextStyle(color = Color.Yellow)
                                            "X" -> TimeTextDefaults.timeTextStyle(color = Color.Red)
                                            else ->  TimeTextDefaults.timeTextStyle(color = Color.Green)
                                        }
                                    )
                                }
                            )
                        },
                        vignette = {
                            Vignette(vignettePosition = VignettePosition.TopAndBottom)
                        },
                        positionIndicator = {
                            PositionIndicator(
                                scalingLazyListState = scalingLazyListState
                            )
                        }
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .focusable()
                                .focusTarget()
                                .fillMaxSize()
                                .rotaryEventHandler(scalingLazyListState),
                            contentPadding = PaddingValues(
                                top = 40.dp,
                                start = 10.dp,
                                end = 10.dp,
                                bottom = 40.dp
                            ),
                            verticalArrangement = Arrangement.Center,
                            state = scalingLazyListState
                        ) {
                            item {
                                //Only Show Notify-Text if its not empty
                                if (!csNotify.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentSize(align = Alignment.Center)
                                            .padding(top = 40.dp),
                                        text = csNotify
                                    )
                                }
                            }
                            items(lChips) { myChip ->
                                when (myChip.nType ) {
                                    1 -> {   //############## Switch
                                        ToggleChip(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                //.height(240.dp)
                                                .padding(top = 4.dp),
                                            appIcon = {
                                                var cStateON: Color = Color.Unspecified
                                                //Check if Icon present
                                                if (!(myChip.sIconB64.isEmpty()) and (myChip.sIconB64 != "null")) {
                                                    //Check if ON-Color is provided
                                                    if ((myChip.sVal.toBoolean()) and (!myChip.sColorOn.isEmpty()))
                                                        try {
                                                            cStateON = Color(android.graphics.Color.parseColor(myChip.sColorOn))
                                                        }catch (e: java.lang.Exception) {}
                                                Icon(
                                                        bitmap = myChip.sIconB64.decodeBase64IntoBitmap(),
                                                        contentDescription = "Custom",
                                                        tint = cStateON ,
                                                        modifier = Modifier
                                                            .padding(top = 3.dp, bottom = 3.dp)
                                                            .wrapContentSize(align = Alignment.Center),
                                                    )
                                                }
                                            },
                                            toggleIcon = {
                                                ToggleChipDefaults.SwitchIcon(checked = myChip.sVal.toBoolean())
                                            },
                                            checked = true,
                                            label = {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = Color.White,
                                                    text = myChip.sName
                                                )
                                            },
                                            enabled = myChip.bWriteable,
                                            onCheckedChange = {
                                                hHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                mSocket.emit("setState", myChip.sStateID, (!myChip.sVal.toBoolean()).toString())
                                                Thread.sleep(200)  //Prevent Multiclick <- Compose Bug?
                                            }
                                        )
                                    }
                                    2 -> {   //############# Level / Slider
                                        Chip(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            icon = {
                                                if (!(myChip.sIconB64.isEmpty()) and (myChip.sIconB64 != "null")) {
                                                    Icon(
                                                        bitmap = myChip.sIconB64.decodeBase64IntoBitmap(),
                                                        contentDescription = "Custom",
                                                        modifier = Modifier
                                                            .padding(top = 3.dp, bottom = 3.dp)
                                                            .wrapContentSize(align = Alignment.Center),
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentSize(align = Alignment.TopStart),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    text = myChip.sName
                                                )
                                            },
                                            secondaryLabel = {
                                                if (myChip.sVal.isDigitsOnly()) {
                                                    Slider(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentSize(align = Alignment.BottomEnd),
                                                        value = myChip.sVal.toFloat(),
                                                        onValueChange = {
                                                            lChips[myChip.nIndex] = myChip.copy(sVal = it.toInt().toString())
                                                            lifecycleScope.launch {
                                                                fSliderChanged(myChip.sStateID,it.toInt(), myChip.nIndex)
                                                            }
                                                        },
                                                        enabled = myChip.bWriteable,
                                                        valueRange = myChip.nMinMax,
                                                        colors = SliderDefaults.colors(
                                                            activeTickColor = Color(
                                                                android.graphics.Color.parseColor(
                                                                    myChip.sColorOn
                                                                )
                                                            ),
                                                            inactiveTickColor = Color(
                                                                android.graphics.Color.parseColor(
                                                                    myChip.sColorOn
                                                                )
                                                            ),
                                                            inactiveTrackColor = Color(
                                                                android.graphics.Color.parseColor(
                                                                    myChip.sColorOn
                                                                )
                                                            ),
                                                            activeTrackColor = Color(
                                                                android.graphics.Color.parseColor(
                                                                    myChip.sColorOn
                                                                )
                                                            ),
                                                            thumbColor = Color(
                                                                android.graphics.Color.parseColor(
                                                                    myChip.sColorOn
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            },
                                            onClick = {
                                                sNotify.value = myChip.sVal
                                            },
                                            enabled = myChip.bWriteable,
                                            colors = ChipDefaults.primaryChipColors(
                                                backgroundColor = Color(32, 33, 36, 255)
                                            )
                                        )
                                    }
                                    else -> {     //############# Label
                                        Chip(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            icon = {
                                                if (!(myChip.sIconB64.isEmpty()) and (myChip.sIconB64 != "null")) {
                                                    Icon(
                                                        bitmap = myChip.sIconB64.decodeBase64IntoBitmap(),
                                                        contentDescription = "Custom",
                                                        modifier = Modifier
                                                            .padding(top = 3.dp, bottom = 3.dp)
                                                            .wrapContentSize(align = Alignment.Center),
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentSize(align = Alignment.TopStart),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    text = myChip.sName
                                                )
                                            },
                                            secondaryLabel = {
                                                Text (
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentSize(align = Alignment.BottomEnd),
                                                    color = Color.Cyan,
                                                    maxLines = 1,
                                                    text = if ((myChip.sUnit.isEmpty()) or (myChip.sUnit == "null")) myChip.sVal else myChip.sVal + " " + myChip.sUnit
                                                )
                                            },
                                            onClick = {

                                            },
                                            enabled = false,
                                            colors = ChipDefaults.primaryChipColors(
                                                backgroundColor = Color(32, 33, 36, 255)
                                            )
                                        )
                                    }
                                }
                            }
                            item {
                                Chip(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    label = {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentSize(align = Alignment.TopStart),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            text = "ioBroker-URL"
                                        )
                                    },
                                    secondaryLabel = {
                                        Text (
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentSize(align = Alignment.BottomEnd),
                                            color = Color.Cyan,
                                            maxLines = 1,
                                            text = csUrl
                                        )
                                    },
                                    onClick = {
                                        val iRemInp: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                                        val lRemInp: List<RemoteInput> = listOf(
                                            RemoteInput.Builder("url")
                                                .setLabel("ioBroker URL http://xxx:8084")
                                                .build())
                                        RemoteInputIntentHelper.putRemoteInputsExtra(iRemInp, lRemInp)
                                        launcher.launch(iRemInp)
                                    },
                                    enabled = true,
                                    colors = ChipDefaults.imageBackgroundChipColors(
                                        backgroundImagePainter = painterResource(id = R.drawable.wrench)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        //Check if configuration is present
        sUrl.value = this.getPreferences(Context.MODE_PRIVATE).getString("sUrl","").toString()
        if (sUrl.value.isEmpty()) {
            sNotify.value = "Bitte Server und Port angeben! \r\n Swipe nach rechts zum beenden ist deaktiviert!"
            bIsLoading.value = false
        } else {
            //Connect to IOBroker and Callback Handler & //Subscribe to IDs to get the States
            if (fSetupSocket()) fSetupList()
        }
    }

    fun fSetupSocket(): Boolean {
        //Connect to ioBroker
        if (!SocketHandler.setSocket(sUrl.value)) return false
        SocketHandler.establishConnection().connected()
        mSocket = SocketHandler.getSocket()
        mSocket.open()

        //Handle Connection
        mSocket.on("connect") {
            sIndicator.value = "✓"
            //Tell ioBroker our name
            mSocket.emit("name", "wearos.0");
        }
        mSocket.on(Socket.EVENT_RECONNECT) {
            //If reconnect is successfull, subscribe all state since they are gone on disconnect!
            for (oChip in lChips) {
                //Subscribe each ID for changes -> mSocket.on("stateChange") called
                mSocket.emit("subscribe", oChip.sStateID)
            }
        }
        mSocket.on(Socket.EVENT_DISCONNECT) {
            sIndicator.value = "X"
        }
        mSocket.on("disconnect") {
            sIndicator.value = "X"
        }
        mSocket.on(Socket.EVENT_CONNECT_ERROR) {
            sIndicator.value = "X"
            //***LOG
        }
        //Handle State Changes
        mSocket.on("stateChange") { resState ->
            val sStateID = resState[0].toString().replace("\"", "")
            val jsState = Json.parseToJsonElement(resState[1].toString()).jsonObject
            val sVal = jsState["val"].toString().replace("\"", "")
            val nPos = lChips.indexOfFirst { it.sStateID == sStateID }

            if ((lChips[nPos].nType == 2) and (bSliderCoolDown)) { //Slider
                //Do Nothing, Cooldown Slider
            }else{
                lChips[nPos].sVal = sVal
                fTriggerChipListComposeUpdate(nPos)
            }
        }
        return true
    }

    fun fSetupList(){
        //Fetch WearOS enum to get all Object/StateID's
        mSocket.emit("getObject", "enum.rooms.WearOS", Ack { resGetObject ->
            try {
                //Get enum member ID's from json
                val jsEnums = Json.parseToJsonElement(resGetObject[1].toString()).jsonObject
                val jaIDs = jsEnums["common"]!!.jsonObject["members"]!!.jsonArray
                //Loop through each ID
                for (sID in jaIDs) {
                    //Remove " from ID's, they are hurting us!
                    val sRawID = sID.toString().replace("\"","")

                    //Subscribe each ID for changes -> mSocket.on("stateChange") called
                    mSocket.emit("subscribe", sRawID)

                    //Create ListItem
                    fAddChip(sRawID)
                }
                bIsLoading.value = false
            }catch (e: Exception){
                sNotify.value = "Kein WearOS Raum gefunden!"
                bIsLoading.value = false
                //***LOG
            }
        })
    }

    fun fAddChip(sID: String){
        //Get Object Information's for ID
        mSocket.emit("getObject", sID, Ack { resGetObject ->
            val jsEnums = Json.parseToJsonElement(resGetObject[1].toString()).jsonObject  //JsonObject Result

            //Define Chip and set values
            val newChip = dcIOChips()
            newChip.nIndex = lChips.lastIndex +1
            newChip.sName = jsEnums["common"]!!.jsonObject["name"].toString().replace("\"","")
            newChip.sVal = "AppErr"
            newChip.sStateID = sID
            newChip.sUnit = jsEnums["common"]!!.jsonObject["unit"].toString().replace("\"","")
            newChip.sIconB64 = jsEnums["common"]!!.jsonObject["icon"].toString().replace("\"","")
            newChip.bWriteable  = jsEnums["common"]!!.jsonObject["write"].toString().replace("\"","").toBoolean()
            newChip.sColorOn = jsEnums["common"]!!.jsonObject["color"].toString().replace("\"","")
            newChip.nMinMax = fGetSliderRange(jsEnums["common"]!!.jsonObject["min"].toString().replace("\"",""), jsEnums["common"]!!.jsonObject["max"].toString().replace("\"",""))
            //Get Object-Role and set Type
            with(jsEnums["common"]!!.jsonObject["role"].toString().replace("\"", "")) {
                when  {
                    startsWith("switch") -> newChip.nType = 1 //ToggleChip
                    startsWith("level") -> newChip.nType = 2  //Slider
                    else -> {
                        newChip.nType = 0   //Chip
                    }
                }
            }
            //Add Chip to ChipList
            lChips.add(newChip)

            //Get State Values, we need to this after the chips has been generated, since the getStates-emit is async, IT IS WHAT IT IS
            fGetVal(sID,lChips.lastIndex)
        })
    }

    fun fGetVal(sID: String, nPos: Int) {
        //Get ID to String Array
        mSocket.emit("getStates", sID, Ack { resGetStates ->
            val jsState = Json.parseToJsonElement(resGetStates[1].toString()).jsonObject
            lChips[nPos].sVal = jsState[sID]!!.jsonObject["val"].toString().replace("\"", "")
            fTriggerChipListComposeUpdate(nPos)
        })
    }


    fun fGetSliderRange(sMin:String,sMax:String): ClosedFloatingPointRange<Float> {
        var nMin: Float = -1f
        var nMax: Float = -1f
        sMin.toFloatOrNull().let {
            if (it != null) {
                nMin = it
            }
        }
        sMax.toFloatOrNull().let {
            if (it != null) {
                nMax = it
            }
        }
        try {
            return nMin.rangeTo(nMax)
        }catch (e:java.lang.Exception){}
        return (-1f).rangeTo(-1f)
    }

    suspend fun fSliderChanged(sStateID: String, nVal: Int, nIndex: Int) {
        if (!bSliderCoolDown) {
            bSliderCoolDown = true
            mSocket.emit("setState", sStateID, nVal.toString())    //No decimal
            delay(2000)
            bSliderCoolDown = false
            mSocket.emit("setState", sStateID, lChips[nIndex].sVal.toString())    //No decimal
        }
    }


    //Helper-Function to Convert Base64 Icons from ioBroker to ImageBitmap
    fun String.decodeBase64IntoBitmap(): ImageBitmap {
        try {
            val byIcon = this.substring(this.indexOf(",") + 1)
            val imageBytes = Base64.decode(byIcon, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
        } catch (e: Exception) {
            //If there's a error show red X
            val sRedXB64 = "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"
            val imageBytes = Base64.decode(sRedXB64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
        }
    }

    /*override fun onDestroy() {
        super.onDestroy()
        //Compose wont update or is laggy after stop&resume, 4 example if display goes to sleep and resume
        //Workaround... Close that shit
        SocketHandler.closeConnection()
    }*/
}

