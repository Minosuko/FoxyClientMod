package com.foxybot.api.process;

import java.util.function.Predicate;
import net.minecraft.entity.Entity;

public interface IFollowProcess extends IBaritoneProcess {
    void follow(Predicate<Entity> filter);
}
