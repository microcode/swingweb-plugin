package se.microcode.confluence.plugin.base;

import se.microcode.cogwork.EventState;

public class EventsMacroArguments
{
    public EventsMacroArguments()
    {
        type = DisplayType.VELOCITY;
        minLevel = -1;
        maxLevel = -1;
        limit = -1;
        sort = SortOrder.OFF;
        started = StartedState.DONT_CARE;
    }

    public DisplayType type;
    public String url;
    public int[] hideCategories;
    public int[] showCategories;
    public EventState[] hideStates;
    public EventState[] showStates;
    public int minLevel;
    public int maxLevel;
    public int limit;
    public SortOrder sort;
    public StartedState started;
}
