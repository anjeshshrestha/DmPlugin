package com.deeme.types;

import com.github.manolo8.darkbot.Main;

import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;

import java.util.Collection;
import java.util.Comparator;

public class SharedFunctions {

    public static Ship getAttacker(Ship assaulted, Main main) {
        HeroAPI hero = main.pluginAPI.getAPI(HeroAPI.class);
        EntitiesAPI entities = main.pluginAPI.getAPI(EntitiesAPI.class);
        return getAttacker(assaulted, entities.getShips(), hero);
    }

    public static Ship getAttacker(Ship assaulted, Collection<? extends Ship> allShips, HeroAPI hero) {
        if (allShips == null || allShips.size() <= 0) {
            return null;
        }

        return allShips.stream()
                .filter(s -> (s instanceof Npc || s.getEntityInfo().isEnemy()))
                .filter(s -> !isPet(s.getEntityInfo().getUsername()))
                .filter(s -> s.isAttacking(assaulted))
                .sorted(Comparator.comparingDouble(s -> s.getLocationInfo().distanceTo(hero)))
                .findFirst().orElse(null);
    }

    public static boolean hasAttacker(Ship assaulted, Main main) {
        Ship ship = getAttacker(assaulted, main);
        return ship != null;
    }

    public static boolean isPet(String name) {
        return name.matches(".*?(\\s)(\\[(\\d+)\\])");
    }

}
