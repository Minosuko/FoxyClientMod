package com.foxyclient.util;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public class FadingSoundInstance extends MovingSoundInstance {
    private float targetVolume;
    private float fadeRate = 0.05F;

    public FadingSoundInstance(SoundEvent sound, SoundCategory category, double x, double y, double z, float initialVolume) {
        super(sound, category, Random.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = initialVolume;
        this.targetVolume = initialVolume;
        this.pitch = 1.0F;
        this.repeat = true;
    }

    public void triggerFadeOut() {
        this.targetVolume = 0.01F;
    }

    public void triggerFadeIn() {
        this.targetVolume = 1.0F;
    }

    public void forceStop() {
        this.setDone();
    }

    @Override
    public void tick() {
        if (this.volume < this.targetVolume) {
            this.volume += this.fadeRate;
            if (this.volume > this.targetVolume) this.volume = this.targetVolume;
        } else if (this.volume > this.targetVolume) {
            this.volume -= this.fadeRate;
            if (this.volume < this.targetVolume) this.volume = this.targetVolume;
        }

        if (this.volume < 0.01F) {
            this.volume = 0.01F;
        }
    }
}
