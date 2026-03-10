package com.foxybot.api.process;

public interface IMineProcess extends IBaritoneProcess {
    void mineByName(String... names);
    void mineByName(int quantity, String... names);
}
