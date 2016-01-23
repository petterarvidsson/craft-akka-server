package konstructs.forest;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import akka.actor.ActorRef;
import akka.actor.Props;

import konstructs.utils.BlockMachine;
import konstructs.utils.DeterministicProductionRule;
import konstructs.utils.LSystem;
import konstructs.utils.ProbabilisticProductionRule;
import konstructs.utils.ProbalisticProduction;
import konstructs.utils.ProductionRule;
import konstructs.plugin.KonstructsActor;
import konstructs.api.*;

class Tree extends KonstructsActor {
    private static final LSystem SYSTEM = getLSystem();
    private static final BlockMachine MACHINE = getBlockMachine();
    private static final String INITIAL_STATE = "a[&[c][-c][--c][+c]]c";
    private static final int INITIAL_DELAY = 1;
    private static final int RANDOM_DELAY = 1;
    private static final int MAX_SEEDS = 5;
    private static final BlockFilter FOREST_BLOCKS = BlockFilterFactory
        .withNamespace("org/konstructs/forest")
        .or(BlockFilterFactory.vacuum())
        .or(BlockFilterFactory
            .withNamespace("org/konstructs")
            .withName("wood"))
        .or(BlockFilterFactory
            .withNamespace("org/konstructs")
            .withName("leaves"));

    private final Random r = new Random();
    private final int maxGenerations;
    private Position position;
    private String state;

    public Tree(ActorRef universe, Position sapling, int maxGenerations) {
        super(universe);
        this.position = sapling;
        this.state = INITIAL_STATE;
        this.maxGenerations = maxGenerations;
        System.out.println("Let's grow a tree");
        viewBlock(sapling);
    }

    private int nextRandomSeedDistance() {
        return - CanATreeGrowHere.CROWN_RADI * 2 + r.nextInt(CanATreeGrowHere.CROWN_RADI * 4);
    }

    private void scheduleGrowth(int generation) {
        if(generation > maxGenerations) {
            /* Tree is done */
            System.out.println("Done!");
            for(int i = 0; i < MAX_SEEDS; i++) {
                Position p = new Position(position.x() + nextRandomSeedDistance(),
                                          position.y(),
                                          position.z() + nextRandomSeedDistance());
                getContext().parent().tell(new TryToSeedTree(p), getSelf());
            }
            getContext().stop(getSelf());
        } else {
            System.out.println("Need more grow");
            /* Tree need to grow more */
            scheduleSelfOnce(new GrowTree(generation),
                             INITIAL_DELAY * 1000 + new Random().nextInt(RANDOM_DELAY) * 1000);
        }
    }

    private void grow(int generation) {
        Map<Position, BlockTypeId> removeOldBlocks =
            BlockMachine.vacuumMachine().interpretJava(state, position);
        state = SYSTEM.iterate(state);
        removeOldBlocks.putAll(MACHINE.interpretJava(state, position));
        replaceBlocks(removeOldBlocks, FOREST_BLOCKS);
        scheduleGrowth(generation + 1);
    }

    public void onBlockViewed(BlockViewed block) {

        if(block.block().type().equals(Forest.SAPLING_ID)) {
            System.out.println("Sapling found!");
            scheduleGrowth(0);
        } else {
            System.out.println("No sapling!");
            getContext().stop(getSelf()); /* Someone removed the sapling */
        }
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof GrowTree) {
            System.out.println("Growing");
            GrowTree growTree = (GrowTree)message;
            grow(growTree.getGeneration());
        } else {
            super.onReceive(message); // Handle konstructs messages
        }
    }


    public static Props props(ActorRef universe, Position start, int maxGenerations) {
        return Props.create(Tree.class, universe, start, maxGenerations);
    }

    private static LSystem getLSystem() {
        List<ProductionRule> rules = new ArrayList<ProductionRule>();

        rules.add(new DeterministicProductionRule("cc",
                    "c[&[c][-c][--c][+c]]c[&[c][-c][--c][+c]]"));
        rules.add(new DeterministicProductionRule("a", "aa"));

        List<ProbalisticProduction> leafGrowthDirections = new ArrayList<ProbalisticProduction>();
        leafGrowthDirections.add(new ProbalisticProduction(20, "c[&[d]]"));
        leafGrowthDirections.add(new ProbalisticProduction(20, "c[&[+d]]"));
        leafGrowthDirections.add(new ProbalisticProduction(20, "c[&[-d]]"));
        leafGrowthDirections.add(new ProbalisticProduction(20, "c[&[--d]]"));
        leafGrowthDirections.add(new ProbalisticProduction(20, "cc"));
        rules.add(ProbabilisticProductionRule.fromList("c", leafGrowthDirections));

        List<ProbalisticProduction> trunkGrowth = new ArrayList<ProbalisticProduction>();
        trunkGrowth.add(new ProbalisticProduction(40, "a[&[c][-c][--c][+c]]"));
        trunkGrowth.add(new ProbalisticProduction(60, "bbba"));
        rules.add(ProbabilisticProductionRule.fromList("aa", trunkGrowth));

        return LSystem.fromList(rules);
    }

    private static BlockMachine getBlockMachine() {
        Map<Character, BlockTypeId> blockMapping = new HashMap<Character, BlockTypeId>();
        blockMapping.put('a', Forest.WOOD_ID);
        blockMapping.put('b', Forest.WOOD_ID);
        blockMapping.put('c', Forest.LEAVES_ID);
        blockMapping.put('d', Forest.LEAVES_ID);
        return BlockMachine.fromJavaMap(blockMapping);
    }
}
