package simple.robot.managers.randomevents;

import net.runelite.api.coords.WorldPoint;
import simple.hooks.scripts.Category;
import simple.hooks.scripts.ScriptManifest;
import simple.hooks.simplebot.ChatMessage;
import simple.hooks.simplebot.Viewport;
import simple.hooks.wrappers.*;
import simple.robot.api.ClientContext;
import simple.robot.script.Script;
import simple.robot.utils.WorldArea;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScriptManifest(author = "Luke", servers = {"Zenyte"}, category = Category.OTHER, description = "Completes Evil Bob random event", discord = "", name = "Evil Bob", version = "1.2")
public class EvilBob extends Script {

    private final int SLAVE_NPC_ID = 392;
    private final int BOB_NPC_ID = 390;
    private final int COOKING_FIRE_ID = 23113;
    private final String UNCOOKED_FISH_ITEM_STRING = "Fishlike thing";
    private final String COOKED_FISH_ITEM_STRING = "raw fishlike thing";
    private final int FISHING_SPOT_ID = 23114;
    private int FISHING_SPOT_DIRECTION = -1;
    private List<Integer> DROPPED_ITEMS = new ArrayList<>();
    private boolean FED_CAT = false;

    private static final WorldArea EVENT_AREA = new WorldArea(new WorldPoint(2495, 4804, 0), new WorldPoint(2559, 4748, 0));

    @Override
    public void onExecute() {
        FISHING_SPOT_DIRECTION = -1;
        DROPPED_ITEMS = new ArrayList<>();
        FED_CAT = false;
        ctx.updateStatus("Starting Evil Bob script...");
    }

    @Override
    public void onProcess() {
        if (checkInvSpots()) {
            return;
        }
        if (FED_CAT) {
            if (!DROPPED_ITEMS.isEmpty()) {
                SimpleItem net = ctx.inventory.populate().filter(303).next();
                if (net != null) {
                    net.click("drop");
                    ctx.sleep(200);
                }
                for (int itemId : DROPPED_ITEMS) {
                    SimpleGroundItem drop = ctx.groundItems.populate().filter(itemId).nearest().next();
                    if (drop != null) {
                        int invSpace = ctx.inventory.getFreeSlots();
                        if (drop.validateInteractable()) {
                            if (drop.click("take")) {
                                ctx.onCondition(() -> ctx.inventory.getFreeSlots() < invSpace);
                            }
                        }
                        if (ctx.inventory.getFreeSlots() < invSpace) {
                            DROPPED_ITEMS.remove(itemId);
                            return;
                        }
                    } else {
                        DROPPED_ITEMS.remove(itemId);
                        return;
                    }
                }
            } else {
                SimpleObject portal = ctx.objects.populate().filter(23115).nearest().next();
                if (portal != null && portal.validateInteractable() && portal.click(1)) {
                    ctx.onCondition(() -> !EvilBob.atEvilBob());
                }
            }
            return;
        }
        if (FISHING_SPOT_DIRECTION != -1 &&
                ctx.inventory.populate().filter(UNCOOKED_FISH_ITEM_STRING).isEmpty() &&
                ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).isEmpty()) {
            if (FISHING_SPOT_DIRECTION == Viewport.SOUTH) {
                if (!ctx.pathing.inArea(2520, 4763, 2530, 4773)) {
                    ctx.pathing.step(2525, 4768);
                    ctx.sleep(3000);
                    return;
                }
            }
            if (FISHING_SPOT_DIRECTION == Viewport.NORTH) {
                if (!ctx.pathing.inArea(2522, 4782, 2532, 4792)) {
                    ctx.pathing.step(2527, 4787);
                    ctx.sleep(3000);
                    return;
                }
            }
            if (FISHING_SPOT_DIRECTION == Viewport.WEST) {
                if (!ctx.pathing.inArea(2508, 4771, 2518, 4781)) {
                    ctx.pathing.step(2513, 4776);
                    ctx.sleep(3000);
                    return;
                }
            }
            if (FISHING_SPOT_DIRECTION == Viewport.EAST) {
                if (!ctx.pathing.inArea(2534, 4773, 2544, 4783)) {
                    ctx.pathing.step(2539, 4778);
                    ctx.sleep(3000);
                    return;
                }
            }
        }
        if (ctx.dialogue.dialogueOpen()) {
            if (processDialogue())
                return;
        }
        if (FISHING_SPOT_DIRECTION == -1) {
            SimpleNpc slave = ctx.npcs.populate().filter(SLAVE_NPC_ID).nearest().next();
            if (slave != null && slave.validateInteractable()) {
                if (slave.click(1)) {
                    ctx.onCondition(() -> ctx.dialogue.dialogueOpen());
                }
            }
        } else if (ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).population() > 0) {
            SimpleNpc bob = ctx.npcs.populate().filter(BOB_NPC_ID).nearest().next();
            if (bob != null && bob.validateInteractable()) {
                SimpleItem cookedFish = ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).next();
                if (cookedFish != null && cookedFish.click("use")) {
                    bob.click(1);
                    if (ctx.onCondition(() -> ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).population() == 0)) {
                        FED_CAT = true;
                    }
                }
            }
        } else if (!ctx.inventory.populate().filter(UNCOOKED_FISH_ITEM_STRING).isEmpty()) {
            SimpleObject fire = ctx.objects.populate().filter(COOKING_FIRE_ID).nearest().next();
            if (fire != null && fire.validateInteractable()) {
                SimpleItem uncookedFish = ctx.inventory.filter(UNCOOKED_FISH_ITEM_STRING).next();
                if (uncookedFish != null && uncookedFish.click("use")) {
                    fire.click(1);
                    ctx.onCondition(() -> ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).population() > 0);
                }
            }
        } else {
            if (ctx.inventory.populate().filter(303).isEmpty()) {
                SimpleGroundItem net = ctx.groundItems.populate().filter(303).nearest().next();
                if (net != null && net.validateInteractable()) {
                    if (net.click("take"))
                        ctx.onCondition(() -> ctx.inventory.populate().filter(303).population() > 0);
                }
            } else {
                SimpleObject spot = ctx.objects.populate().filter(FISHING_SPOT_ID).nearest().next();
                if (spot != null && spot.validateInteractable()) {
                    if (spot.click("net")) {
                        ctx.onCondition(() -> ctx.inventory.populate().filter(UNCOOKED_FISH_ITEM_STRING).population() > 0);
                    }
                }
            }
        }
    }

    public boolean checkInvSpots() {
        if (ctx.inventory.getFreeSlots() < 2 && ctx.inventory.populate().filter(303).isEmpty() &&
                ctx.inventory.populate().filter(COOKED_FISH_ITEM_STRING).isEmpty() &&
                ctx.inventory.populate().filter(UNCOOKED_FISH_ITEM_STRING).isEmpty()) {
            ctx.updateStatus("[BOB] checking inventory slots");
            SimpleItem item = ctx.inventory.populate().next();
            if (item != null) {
                if (!DROPPED_ITEMS.contains(item.getId())) {
                    DROPPED_ITEMS.add(item.getId());
                }
                if (item.click("drop")) {
                    ctx.onCondition(() -> ctx.inventory.populate().filter(item.getId()).population() == 0);
                }
                return true;
            }
        }
        return false;
    }

    public boolean processDialogue() {
        ctx.updateStatus("[BOB] processing dialogue");
        String[] TAM = ctx.dialogue.getDialogueTitleAndMessage();
        //System.out.println("TAM: "+TAM[0]+" & "+TAM[1]);
        SimpleWidget[] options = ctx.dialogue.getDialogueOptions();
        if (!TAM[1].equals("")) {
            String message = TAM[1].toLowerCase();
            if (message.contains("that fishing spot c-c-contains")) {
                ctx.viewport.faceCamera(Viewport.NORTH, true);
                ctx.viewport.pitch(true);
                //if(message.contains("he might f-f-f-fall asleep.")) {
                this.FISHING_SPOT_DIRECTION = getDirection();
                return true;
            }
            SimpleWidget continueButton = ctx.dialogue.getContinueButton();
            continueButton.click(1);
            ctx.sleep(1000);
            return true;
        } else if (options != null) {

        }
        return false;
    }

    public int getDirection() {
        ctx.updateStatus("[BOB] getting direction");
        SimpleWidget continueButton = ctx.dialogue.getContinueButton();
        continueButton.click(1);
        ctx.sleep(2500);
        int x = ctx.camera.x();
        int y = ctx.camera.y();
        //System.out.println("x: "+x+" and y: "+y);
        //auto.sleep(2000);
        if (x == 6848) { //&& y == 5952) {
            return Viewport.SOUTH;
        } else if (x == 6976) {
            return Viewport.NORTH;
        } else if (x == 7488) {
            return Viewport.EAST;
        } else {
            return Viewport.WEST;
        }
    }
    
    public static boolean atEvilBob() {
        return EVENT_AREA.containsPoint(ClientContext.getAPI().players.getLocal().getLocation());
    }

    @Override public void onTerminate() {}

    @Override public void onChatMessage(ChatMessage message) {}

    @Override public void paint(Graphics g) {}

}
