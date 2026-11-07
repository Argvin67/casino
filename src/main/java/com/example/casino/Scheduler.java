package com.example.casino;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Мини-планировщик на тиках сервера (для анимаций). Задачи игрока можно выполнить сразу при выходе. */
final class Scheduler {
    private static final class Task {
        final UUID owner;
        int ticks;
        final Runnable action;

        Task(UUID owner, int ticks, Runnable action) {
            this.owner = owner;
            this.ticks = ticks;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {}

    static void later(net.minecraft.server.network.ServerPlayerEntity p, int ticks, Runnable action) {
        TASKS.add(new Task(p.getUuid(), Math.max(1, ticks), action));
    }

    static void tick() {
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.ticks <= 0) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) run(t);
    }

    /** Выполняет все отложенные задачи игрока немедленно (например, при выходе, чтобы не потерять выплату). */
    static void flush(UUID owner) {
        List<Task> mine = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.owner.equals(owner)) {
                mine.add(t);
                it.remove();
            }
        }
        for (Task t : mine) run(t);
    }

    private static void run(Task t) {
        try {
            t.action.run();
        } catch (Exception e) {
            CasinoMod.LOGGER.error("Casino task failed", e);
        }
    }
}
