package com.foxybot.api;

public final class FoxyBotAPI {
    private static IFoxyBotProvider provider;

    public static IFoxyBotProvider getProvider() {
        if (provider == null) {
            try {
                // Initialize the implementation via reflection or direct instantiation
                provider = (IFoxyBotProvider) Class.forName("com.foxybot.FoxyBotProviderImpl").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize FoxyBot provider", e);
            }
        }
        return provider;
    }

    public static void setProvider(IFoxyBotProvider p) {
        provider = p;
    }
}
