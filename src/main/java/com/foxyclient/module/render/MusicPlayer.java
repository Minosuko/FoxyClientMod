package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.FoxyMusicManager;

public class MusicPlayer extends Module {
    public final NumberSetting volume = addSetting(new NumberSetting("Volume", "Playback mass gain", 1.0, 0.0, 2.0));
    public final BoolSetting restartOnEnable = addSetting(new BoolSetting("Restart", "Restart track on toggle", false));

    public MusicPlayer() {
        super("MusicPlayer", "Plays designated audio tracks in-game", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (restartOnEnable.get()) {
            FoxyMusicManager.stop();
        }
        FoxyMusicManager.play();
    }

    @Override
    public void onDisable() {
        FoxyMusicManager.stop();
    }
}
