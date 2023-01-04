package com.schnup.iobrokerw

import android.annotation.SuppressLint
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import com.schnup.iobrokerw.ui.theme.IoBrokerWTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.json.JSONArray
import org.json.JSONObject
import kotlin.system.exitProcess


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
    var sColorBgnd: String = "null",
    var nMinMax: ClosedFloatingPointRange<Float> = (0f).rangeTo(100f),
    var nType: Int = 0,                     //0 = Chip, 1 = ToggleChip, 2 = Slider  Unknown = Chip
    var bUpdateCompose: Boolean = false     //Used for Change value to trigger Compose Update
)

class MainActivity : ComponentActivity(), MessageListener {
    private val sIndicator = MutableStateFlow("?")
    private val sNotify = MutableStateFlow("")
    private val sUrl = MutableStateFlow("")
    private val lChips = mutableStateListOf<dcIOChips>()
    private var bIsLoading = MutableStateFlow(true)
    private var bSliderCoolDown = false

    //Compose update triggers automaticly if a chip is added oder removed, but not on changes inside the chip (dcIOChip-class)
    private fun fTriggerChipListComposeUpdate(nIndex: Int){
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
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()


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
                            //if (fSetupSocket()) fSetupList()
                            fSetupSocket()
                        }
                    }
                }

                IoBrokerWTheme {
                    val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    Scaffold(
                        modifier = Modifier
                            .background(Color.Black),
                        timeText = {
                            TimeText(
                                startCurvedContent = {
                                    curvedText(
                                        text = csIndicator, //✔
                                        fontWeight = FontWeight.Bold,
                                        style =
                                        when (csIndicator) {
                                            "?" -> CurvedTextStyle(color = Color.Yellow)
                                            "X" -> CurvedTextStyle(color = Color.Red)
                                            else -> CurvedTextStyle(color = Color.Green)
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
                                .fillMaxSize()
                                .onPreRotaryScrollEvent {
                                    coroutineScope.launch {
                                        //Scroll with Rotary
                                        scalingLazyListState.animateScrollBy(it.verticalScrollPixels,
                                            //Smooth Scrolling
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = LinearOutSlowInEasing
                                            )
                                        )
                                    }
                                    true
                                }
                                .focusRequester(focusRequester)
                                .focusable(),
                            contentPadding = PaddingValues(
                                top = 40.dp,
                                start = 10.dp,
                                end = 10.dp,
                                bottom = 0.dp
                            ),
                            verticalArrangement = Arrangement.Center,
                            state = scalingLazyListState
                        ) {
                            if (csNotify.isNotEmpty()) {
                                item {
                                    //Only Show Notify-Text if its not empty
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
                                        var cBgnd: Color = Color.DarkGray
                                        if (myChip.sColorBgnd != "null") cBgnd = Color(android.graphics.Color.parseColor(myChip.sColorBgnd))
                                        ToggleChip(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            colors =
                                                ToggleChipDefaults.toggleChipColors(
                                                    checkedStartBackgroundColor = cBgnd
                                                ),
                                                appIcon = {
                                                    var cStateON: Color = Color.Unspecified
                                                    //Check if Icon present
                                                    if ((myChip.sIconB64.isNotEmpty()) and (myChip.sIconB64 != "null")) {
                                                        //Check if ON-Color is provided and value is ON
                                                        if (myChip.sVal.toBoolean()) cStateON = Color(android.graphics.Color.parseColor(myChip.sColorOn))
                                                        Icon(
                                                            bitmap = myChip.sIconB64.decodeBase64IntoBitmap(),
                                                            contentDescription = "Custom",
                                                            tint = cStateON,
                                                            modifier = Modifier
                                                                .padding(top = 3.dp, bottom = 3.dp)
                                                                .wrapContentSize(align = Alignment.Center),
                                                        )
                                                    }
                                                },
                                                toggleControl = {
                                                    Switch(checked = myChip.sVal.toBoolean())
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
                                                    WebSocketManager.sendMessage(JSONArray(listOf(3,
                                                        WebSocketManager.nWSID,"setState",JSONArray(listOf(myChip.sStateID,(!myChip.sVal.toBoolean()))))).toString())
                                                },
                                        )
                                    }
                                    2 -> {   //############# Level / Slider
                                        var cBgnd: Color = Color.DarkGray
                                        if (myChip.sColorBgnd != "null") cBgnd = Color(android.graphics.Color.parseColor(myChip.sColorBgnd))
                                        Chip(
                                            colors = ChipDefaults.primaryChipColors(
                                                backgroundColor = cBgnd
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            icon = {
                                                if ((myChip.sIconB64.isNotEmpty()) and (myChip.sIconB64 != "null")) {
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
                                                        colors =
                                                        SliderDefaults.colors(
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
                                            onClick = {},
                                            enabled = myChip.bWriteable,
                                        )
                                    }
                                    3 -> {   //############## Switch PressButton
                                        var cBgnd: Color = Color.DarkGray
                                        if (myChip.sColorBgnd != "null") cBgnd = Color(android.graphics.Color.parseColor(myChip.sColorBgnd))
                                        if (isPressed) {
                                            cBgnd = Color.Yellow
                                            if (myChip.sVal == "false") {
                                                WebSocketManager.sendMessage(JSONArray(listOf(3,
                                                    WebSocketManager.nWSID,"setState",JSONArray(listOf(myChip.sStateID,"true")))).toString())
                                                myChip.sVal = "true"
                                            }
                                            //Use if + DisposableEffect to wait for the press action is completed
                                            DisposableEffect(Unit) {
                                                onDispose {
                                                    WebSocketManager.sendMessage(JSONArray(listOf(3,
                                                        WebSocketManager.nWSID,"setState",JSONArray(listOf(myChip.sStateID,"false")))).toString())
                                                    myChip.sVal = "false"
                                                }
                                            }
                                        }
                                        Chip(
                                            colors = ChipDefaults.primaryChipColors(
                                                backgroundColor = cBgnd,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            icon = {
                                                if ((myChip.sIconB64.isNotEmpty()) and (myChip.sIconB64 != "null")) {
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
                                            onClick = {
                                                //Nothing
                                            },
                                            interactionSource = interactionSource,
                                            enabled = true
                                        )
                                    }
                                    else -> {     //############# Label
                                        var cBgnd: Color = Color.DarkGray
                                        if (myChip.sColorBgnd != "null") {
                                            cBgnd = Color(android.graphics.Color.parseColor(myChip.sColorBgnd))
                                        }
                                        Chip(
                                            colors = ChipDefaults.primaryChipColors(
                                                backgroundColor = cBgnd
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            icon = {
                                                if ((myChip.sIconB64.isNotEmpty()) and (myChip.sIconB64 != "null")) {
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
                                                //Nothing
                                            },
                                            enabled = false
                                        )
                                    }
                                }
                            }
                            item {              //############# Chip Settings
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
                            item {              //############# Chip Exit
                                Chip(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    label = {
                                    },
                                    secondaryLabel = {
                                    },
                                    onClick = {
                                        exitProcess(0)
                                    },
                                    enabled = true,
                                    colors = ChipDefaults.imageBackgroundChipColors(
                                        backgroundImagePainter = painterResource(id = R.drawable.exitt)
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
            //Connect to IOBroker and Callback Handler & Subscribe to IDs to get the State changes
            fSetupSocket()
        }
    }

    private fun fSetupSocket(): Boolean {
        //Connect to ioBroker
        if (sUrl.value.endsWith("/?sid=admin")) sUrl.value = sUrl.value.dropLast(11)    //Remove sid from url and save it (Workaround old Version)
        if (WebSocketManager.isConnect()) WebSocketManager.close()
        if (!WebSocketManager.init(sUrl.value + "/?sid=admin", this)) return false
        if (!WebSocketManager.connect()) return false

        Log.d(WebSocketManager.TAG, "Main: Connect OK")
        return true
    }

    override fun onConnectSuccess() {
        val arg = JSONArray(listOf("WearOS"))
        WebSocketManager.sendMessage(JSONArray(listOf(3,WebSocketManager.nWSID,"name",arg)).toString())
        sIndicator.value = "✓"
    }

    override fun onConnectFailed() {
        sIndicator.value = "X"
        Log.d(WebSocketManager.TAG, "Connection Failed")
    }

    override fun onClose() {
        sIndicator.value = "X"
        Log.d(WebSocketManager.TAG, "Connection Closed")
    }

    override fun onMessage(sMsg: String?) {
        val jMsg: JSONArray
        //Log.d(WebSocketManager.TAG, "onMsg: " + sMsg)
        try {
            jMsg = JSONArray(sMsg)
        }catch (e:Exception){
            Log.d(WebSocketManager.TAG, "onMsg: Error Msg to Json: " + e.message)
            return
        }
        when (jMsg[0]) {    //Check Message-Type
            1 -> WebSocketManager.sendMessage(JSONArray(listOf(2)).toString())          //On Ping send Pong
            0,3 -> {                                                                    //On Message
                when (jMsg[2]) {                                                        //Check Message Topic
                    "___ready___" -> {                                                  //On First "Ready" get WearOS Enums    //Fetch WearOS enum to get all Object/StateID's
                        WebSocketManager.sendCallback("getObject","enum.rooms.WearOS")
                    }
                    "getObject" -> {
                        val jArg = JSONObject(JSONArray(jMsg[3].toString())[1].toString())
                        when (jArg["_id"]) {
                            "enum.rooms.WearOS" -> fSetupList(JSONArray(jMsg[3].toString()))
                            else -> fAddChip(JSONArray(jMsg[3].toString()))
                        }
                    }
                    "getStates" -> {
                        fGetVal(JSONArray(jMsg[3].toString()))
                    }
                    "stateChange" -> {
                        fStateChange(JSONArray(jMsg[3].toString()))
                    }
                }
            }
        }
    }

    private fun fSetupList(jRes: JSONArray){
        //Fetch WearOS enum to get all Object/StateID's
        try {
            //if (!lChips.isEmpty()) return
            //Get enum member ID's from json
            val jsEnums = Json.parseToJsonElement(jRes[1].toString()).jsonObject
            val jaIDs = jsEnums["common"]!!.jsonObject["members"]!!.jsonArray
            //Loop through each ID
            for (sID in jaIDs) {
                //Remove " from ID's, they are hurting us!
                val sRawID = sID.toString().replace("\"","")

                //Subscribe each ID for changes
                WebSocketManager.sendCallback("subscribe",sRawID)
                WebSocketManager.sendCallback("getObject",sRawID)
            }
            bIsLoading.value = false
        }catch (e: Exception){
            sNotify.value = "Kein WearOS Raum gefunden!"
            bIsLoading.value = false
        }
    }

    private fun fAddChip(jRes: JSONArray){
        //Get Object Information's for ID
        val jsEnums = Json.parseToJsonElement(jRes[1].toString()).jsonObject  //JsonObject Result
        var sID = jsEnums["_id"].toString()
        //Remove " from ID's, they are hurting us!
        sID = sID.replace("\"","")

        //Define Chip and set values
        val newChip = dcIOChips()
        newChip.nIndex = lChips.lastIndex +1
        newChip.sName = jsEnums["common"]!!.jsonObject["name"].toString().replace("\"","")
        newChip.sVal = "AppErr"
        newChip.sStateID = sID
        newChip.sUnit = jsEnums["common"]!!.jsonObject["unit"].toString().replace("\"","")
        newChip.sIconB64 = jsEnums["common"]!!.jsonObject["icon"].toString().replace("\"","")
        newChip.bWriteable  = jsEnums["common"]!!.jsonObject["write"].toString().replace("\"","").toBoolean()
        newChip.sColorOn = fCheckColorCode(jsEnums["common"]!!.jsonObject["color"].toString().replace("\"",""),"#ffff00")
        newChip.sColorBgnd = fCheckColorCode(jsEnums["common"]!!.jsonObject["color-background"].toString().replace("\"",""),"null")
        newChip.nMinMax = fGetSliderRange(jsEnums["common"]!!.jsonObject["min"].toString().replace("\"",""), jsEnums["common"]!!.jsonObject["max"].toString().replace("\"",""))
        //Get Object-Role and set Type
        with(jsEnums["common"]!!.jsonObject["role"].toString().replace("\"", "")) {
            when  {
                startsWith("switch") -> {
                    if (jsEnums["common"]!!.jsonObject["desc"].toString().replace("\"","") == "press") newChip.nType = 3    //ToggleChip with Press
                    else newChip.nType = 1  //ToggleChip
                }
                startsWith("scene.states") -> newChip.nType = 1 //ToggleChip
                startsWith("level") -> newChip.nType = 2  //Slider
                else -> {
                    newChip.nType = 0   //Chip
                }
            }
        }
        //Add Chip to ChipList
        lChips.add(newChip)
        //Get State Values, we need to this after the chips has been generated, since the getStates-emit is async, IT IS WHAT IT IS
        WebSocketManager.sendCallback("getStates",sID)
    }

    private fun fStateChange(jRes: JSONArray) {
        val sStateID = jRes[0].toString().replace("\"", "")
        Log.d(WebSocketManager.TAG, "statechange: $sStateID")
        val jsState = Json.parseToJsonElement(jRes[1].toString()).jsonObject
        val sVal = jsState["val"].toString().replace("\"", "")
        val nPos = lChips.indexOfFirst { it.sStateID == sStateID }

        if ((lChips[nPos].nType == 2) and (bSliderCoolDown)) { //Slider
            //Do Nothing, Cooldown Slider
        } else {
            lChips[nPos].sVal = sVal
            fTriggerChipListComposeUpdate(nPos)
        }
    }

    private fun fGetVal(jRes: JSONArray) {
        //Get ID to String Array
        val sID = JSONObject(jRes[1].toString()).keys().next()
        val nPos = lChips.indexOfFirst { it.sStateID == sID }
        val jsState = Json.parseToJsonElement(jRes[1].toString()).jsonObject
        lChips[nPos].sVal = jsState[sID]!!.jsonObject["val"].toString().replace("\"", "")
        fTriggerChipListComposeUpdate(nPos)
    }

    private fun fCheckColorCode(sColorCode:String, sDefColor:String): String {
        try {
            val color: Color = Color(android.graphics.Color.parseColor(sColorCode)) // Color valid, set it
            return sColorCode
        } catch (iae: IllegalArgumentException) {
            return sDefColor //"#ffff00" // Color Invalid, set to Yellow
        }
    }

    private fun fGetSliderRange(sMin:String, sMax:String): ClosedFloatingPointRange<Float> {
        var nMin = 0f
        var nMax = 100f
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
        }catch (_:java.lang.Exception){}
        return nMin.rangeTo(nMax)
    }

    private suspend fun fSliderChanged(sStateID: String, nVal: Int, nIndex: Int) {
        if (!bSliderCoolDown) {
            bSliderCoolDown = true
            WebSocketManager.sendMessage(JSONArray(listOf(3,
                WebSocketManager.nWSID,"setState",JSONArray(listOf(sStateID,nVal.toString())))).toString())
            delay(2000)
            bSliderCoolDown = false
            WebSocketManager.sendMessage(JSONArray(listOf(3,
                WebSocketManager.nWSID,"setState",JSONArray(listOf(sStateID, lChips[nIndex].sVal)))).toString())
        }
    }


    //Helper-Function to Convert Base64 Icons from ioBroker to ImageBitmap
    private fun String.decodeBase64IntoBitmap(): ImageBitmap {
        return try {
            val byIcon = this.substring(this.indexOf(",") + 1)
            val imageBytes = Base64.decode(byIcon, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
        } catch (e: Exception) {
            //If there's a error show red X
            val sRedXB64 = "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"
            val imageBytes = Base64.decode(sRedXB64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
        }
    }
}

