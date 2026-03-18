package com.foxyclient.util;

import net.minecraft.entity.player.SkinTextures;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class SkinTexturesDebugger {
    public static void debug() {
        System.out.println("--- SkinTextures Debug ---");
        try {
            Class<?> clazz = SkinTextures.class;
            System.out.println("Class: " + clazz.getName());
            
            System.out.println("Methods:");
            for (Method m : clazz.getDeclaredMethods()) {
                System.out.println("  " + m.getName() + " -> " + m.getReturnType().getSimpleName());
            }
            
            System.out.println("Fields:");
            for (Field f : clazz.getDeclaredFields()) {
                System.out.println("  " + f.getName() + " -> " + f.getType().getSimpleName());
            }
            
            System.out.println("Inner Classes:");
            for (Class<?> c : clazz.getDeclaredClasses()) {
                System.out.println("  " + c.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
