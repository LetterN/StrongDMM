package strongdmm.ui

import imgui.ImBool
import imgui.ImGui
import imgui.ImGui.separator
import imgui.ImGui.text
import org.lwjgl.glfw.GLFW
import strongdmm.byond.TYPE_AREA
import strongdmm.byond.TYPE_MOB
import strongdmm.byond.TYPE_OBJ
import strongdmm.byond.TYPE_TURF
import strongdmm.controller.action.ActionStatus
import strongdmm.controller.shortcut.Shortcut
import strongdmm.controller.shortcut.ShortcutHandler
import strongdmm.event.DmeItemType
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.util.NfdUtil
import strongdmm.util.imgui.mainMenuBar
import strongdmm.util.imgui.menu
import strongdmm.util.imgui.menuItem

class MenuBarUi : EventSender, EventConsumer, ShortcutHandler() {
    private var progressText: String? = null
    private var isEnvironmentOpened: Boolean = false

    private var isUndoEnabled: Boolean = false
    private var isRedoEnabled: Boolean = false

    private val isAreaLayerActive: ImBool = ImBool(true)
    private val isTurfLayerActive: ImBool = ImBool(true)
    private val isObjLayerActive: ImBool = ImBool(true)
    private val isMobLayerActive: ImBool = ImBool(true)

    init {
        consumeEvent(Event.Global.ResetEnvironment::class.java, ::handleResetEnvironment)
        consumeEvent(Event.Global.ActionStatusChanged::class.java, ::handleActionStatusChanged)
        consumeEvent(Event.Global.RefreshLayersFilter::class.java, ::handleRefreshLayersFilter)
        consumeEvent(Event.Global.TriggerShortcut::class.java, ::handleTriggerShortcut)

        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_O, action = ::doOpenMap)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_O, action = ::doOpenAvailableMap)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_S, action = ::doSave)

        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_Z, action = ::doUndo)
        addShortcut(Shortcut.CONTROL_PAIR, Shortcut.SHIFT_PAIR, GLFW.GLFW_KEY_Z, action = ::doRedo)

        // "Manual" methods since toggle through the buttons switches ImBool status vars automatically.
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_1, action = ::toggleAreaLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_2, action = ::toggleTurfLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_3, action = ::toggleObjLayerManual)
        addShortcut(Shortcut.CONTROL_PAIR, GLFW.GLFW_KEY_4, action = ::toggleMobLayerManual)
    }

    fun process() {
        mainMenuBar {
            menu("File") {
                menuItem("Open Environment...", enabled = progressText == null, block = ::doOpenEnvironment)
                separator()
                menuItem("Open Map...", shortcut = "Ctrl+O", enabled = isEnvironmentOpened, block = ::doOpenMap)
                menuItem("Open Available Map", shortcut = "Ctrl+Shift+O", enabled = isEnvironmentOpened, block = ::doOpenAvailableMap)
                separator()
                menuItem("Save", shortcut = "Ctrl+S", enabled = isEnvironmentOpened, block = ::doSave)
            }

            menu("Edit") {
                menuItem("Undo", shortcut = "Ctrl+Z", enabled = isUndoEnabled, block = ::doUndo)
                menuItem("Redo", shortcut = "Ctrl+Shift+Z", enabled = isRedoEnabled, block = ::doRedo)
            }

            menu("Layers") {
                menuItem("Layers Filter", enabled = isEnvironmentOpened, block = ::doOpenLayersFilter)
                menuItem("Area", shortcut = "Ctrl+1", enabled = isEnvironmentOpened, selected = isAreaLayerActive, block = ::toggleAreaLayer)
                menuItem("Turf", shortcut = "Ctrl+2", enabled = isEnvironmentOpened, selected = isTurfLayerActive, block = ::toggleTurfLayer)
                menuItem("Object", shortcut = "Ctrl+3", enabled = isEnvironmentOpened, selected = isObjLayerActive, block = ::toggleObjLayer)
                menuItem("Mob", shortcut = "Ctrl+4", enabled = isEnvironmentOpened, selected = isMobLayerActive, block = ::toggleMobLayer)
            }

            progressText?.let {
                val count = (ImGui.getTime() / 0.25).toInt() and 3
                val bar = charArrayOf('|', '/', '-', '\\')
                text("${bar[count]} $it${".".repeat(count)}")
            }
        }
    }

    private fun doOpenEnvironment() {
        NfdUtil.selectFile("dme")?.let { file ->
            progressText = "Loading " + file.absolutePath.replace('\\', '/').substringAfterLast("/")
            sendEvent(Event.EnvironmentController.Open(file) {
                progressText = null
                isEnvironmentOpened = it
            })
        }
    }

    private fun doOpenMap() {
        if (!isEnvironmentOpened) {
            return
        }

        sendEvent(Event.EnvironmentController.Fetch { environment ->
            NfdUtil.selectFile("dmm", environment.rootPath)?.let { path ->
                sendEvent(Event.MapHolderController.Open(path))
            }
        })
    }

    private fun doOpenAvailableMap() {
        if (isEnvironmentOpened) {
            sendEvent(Event.AvailableMapsDialogUi.Open())
        }
    }

    private fun doSave() {
        if (isEnvironmentOpened) {
            sendEvent(Event.MapHolderController.Save())
        }
    }

    private fun doUndo() {
        if (isUndoEnabled) {
            sendEvent(Event.ActionController.UndoAction())
        }
    }

    private fun doRedo() {
        if (isRedoEnabled) {
            sendEvent(Event.ActionController.RedoAction())
        }
    }

    private fun doOpenLayersFilter() {
        if (isEnvironmentOpened) {
            sendEvent(Event.LayersFilterPanelUi.Open())
        }
    }

    private fun toggleAreaLayer() {
        toggleLayer(isAreaLayerActive, TYPE_AREA)
    }

    private fun toggleAreaLayerManual() {
        if (isEnvironmentOpened) {
            isAreaLayerActive.set(!isAreaLayerActive.get())
            toggleLayer(isAreaLayerActive, TYPE_AREA)
        }
    }

    private fun toggleTurfLayer() {
        toggleLayer(isTurfLayerActive, TYPE_TURF)
    }

    private fun toggleTurfLayerManual() {
        if (isEnvironmentOpened) {
            isTurfLayerActive.set(!isTurfLayerActive.get())
            toggleLayer(isTurfLayerActive, TYPE_TURF)
        }
    }

    private fun toggleObjLayer() {
        toggleLayer(isObjLayerActive, TYPE_OBJ)
    }

    private fun toggleObjLayerManual() {
        if (isEnvironmentOpened) {
            isObjLayerActive.set(!isObjLayerActive.get())
            toggleLayer(isObjLayerActive, TYPE_OBJ)
        }
    }

    private fun toggleMobLayer() {
        toggleLayer(isMobLayerActive, TYPE_MOB)
    }

    private fun toggleMobLayerManual() {
        if (isEnvironmentOpened) {
            isMobLayerActive.set(!isMobLayerActive.get())
            toggleLayer(isMobLayerActive, TYPE_MOB)
        }
    }

    private fun toggleLayer(layerStatus: ImBool, layerType: String) {
        if (layerStatus.get()) {
            sendEvent(Event.LayersFilterController.ShowByType(layerType))
        } else {
            sendEvent(Event.LayersFilterController.HideByType(layerType))
        }
    }

    private fun handleResetEnvironment() {
        isEnvironmentOpened = false
    }

    private fun handleActionStatusChanged(event: Event<ActionStatus, Unit>) {
        isUndoEnabled = event.body.hasUndoAction
        isRedoEnabled = event.body.hasRedoAction
    }

    private fun handleRefreshLayersFilter(event: Event<Set<DmeItemType>, Unit>) {
        isAreaLayerActive.set(!event.body.contains(TYPE_AREA))
        isTurfLayerActive.set(!event.body.contains(TYPE_TURF))
        isObjLayerActive.set(!event.body.contains(TYPE_OBJ))
        isMobLayerActive.set(!event.body.contains(TYPE_MOB))
    }

    private fun handleTriggerShortcut(event: Event<Shortcut, Unit>) {
        handleShortcut(event.body)
    }
}
