package tilemenu

import (
	"github.com/SpaiR/imgui-go"
	"sdmm/app/command"
	"sdmm/app/ui/shortcut"
	"sdmm/dmapi/dmmap"
	"sdmm/dmapi/dmmap/dmmdata/dmmprefab"
	"sdmm/dmapi/dmmap/dmminstance"
	"sdmm/util"
)

type App interface {
	DoUndo()
	DoRedo()

	DoCopy()
	DoPaste()
	DoCut()
	DoDelete()

	DoEditInstance(*dmminstance.Instance)
	DoSelectPrefab(prefab *dmmprefab.Prefab)

	PointSize() float32

	CommandStorage() *command.Storage
	Clipboard() *dmmap.Clipboard
}

type mapState interface {
	Dmm() *dmmap.Dmm
}

type TileMenu struct {
	shortcuts shortcut.Shortcuts

	app      App
	mapState mapState

	opened bool

	tile *dmmap.Tile
}

func New(app App, state mapState) *TileMenu {
	t := &TileMenu{app: app, mapState: state}
	t.addShortcuts()
	return t
}

func (t *TileMenu) Dispose() {
	t.close()
	t.shortcuts.Dispose()
}

func (t *TileMenu) Open(coord util.Point) {
	if t.mapState.Dmm().HasTile(coord) {
		t.tile = t.mapState.Dmm().GetTile(coord)
		t.opened = true
		imgui.OpenPopup("tileMenu")
	}
}

func (t *TileMenu) close() {
	t.opened = false
	t.tile = nil
}
