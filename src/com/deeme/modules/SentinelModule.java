package com.deeme.modules;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.tree.ConfigField;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.objects.swf.group.GroupMember;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Lazy;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.modules.utils.NpcAttacker;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import javax.swing.*;
import java.util.List;
import java.util.Random;

@Feature(name = "Sentinel Module", description = "Follow the main ship or the group leader and do the same")
public class SentinelModule implements Module, Configurable<SentinelModule.SentinelConfig> {
    private SentinelConfig sConfig;
    private Ship sentinel;
    private Main main;
    private int shipIDToFollow = 0;
    private List<Ship> ships;
    private NpcAttacker attacker;
    private List<Npc> npcs;
    private Drive drive;
    private Random rnd;
    private SafetyFinder safety;


    @Override
    public void install(Main main) {
        this.main = main;
        this.ships = main.mapManager.entities.ships;
        this.npcs = main.mapManager.entities.npcs;
        this.attacker = new NpcAttacker(main);
        this.drive = main.hero.drive;
        this.rnd = new Random();
        this.safety = new SafetyFinder(main);
    }

    @Override
    public void uninstall() {
        safety.uninstall();
    }

    @Override
    public boolean canRefresh() {
        return safety.tick();
    }

    @Override
    public void setConfig(SentinelConfig sentinelConfig) {
        this.sConfig = sentinelConfig;
    }

    public static class SentinelConfig  {
        @Option("")
        @Editor(value = jInstructions.class, shared = true)
        public Lazy<String> instruction;

        @Option(value = "Sentinel ID", description = "Main ship ID")
        @Num(max = 2100000000, step = 1)
        public int sentinelID = 0;
    }

    @Override
    public void tick() {
        main.guiManager.pet.setEnabled(true);
        if (main.guiManager.group.group != null && main.guiManager.group.group.isValid()) {
            if (sConfig.sentinelID == 0) {
                shipIDToFollow = getIdToFollow();
            } else {
                shipIDToFollow = sConfig.sentinelID;
            }

            if (shipIDToFollow == 0) return;

            if (shipAround()) {
                if (!isAttacking() && main.hero.target != sentinel) {
                    main.hero.roamMode();
                    drive.move(sentinel);
                } else {
                    drive.move(Location.of(attacker.target.locationInfo.now, rnd.nextInt(360), attacker.target.npcInfo.radius));
                }
            } else {
                goToLeader();
            }

        }
    }

    private boolean isAttacking() {
        if ((attacker.target = this.npcs.stream()
                .filter(s -> sentinel.isAttacking(s))
                .findAny().orElse(null)) == null) {
            return false;
        }
        main.hero.attackMode(attacker.target);
        attacker.doKillTargetTick();

        return (attacker.target != null);
    }

    private boolean shipAround() {
        sentinel = this.ships.stream()
                .filter(ship -> (ship.id == shipIDToFollow))
                .findAny().orElse(null);
        return sentinel != null;
    }

    private void goToLeader() {
        for (GroupMember m : main.guiManager.group.group.members) {
            if (m.isLeader) {
                if (m.mapId == main.hero.map.id) {
                    drive.move(m.location);
                } else {
                    main.setModule(new MapModule()).setTarget(main.starManager.byId(m.mapId));
                }
                return;
            }
        }
    }

    private int getIdToFollow() {
        for (GroupMember m : main.guiManager.group.group.members) {
            if (m.isLeader && m.id != main.hero.id) {
                return m.id;
            }
        }

        return 0;
    }

    public static class jInstructions extends JPanel implements OptionEditor {
        public jInstructions() {
            JLabel text = new JLabel("<html>Sentinel Module: <br/>" +
            "It's important that the main ship is in a group <br/>" +
                    "If a \"Sentinel ID\" is not defined, it will follow the group leader</html>");
            add(text);
        }

        public JComponent getComponent() { return this; }

        public void edit(ConfigField configField) {}
    }

}
