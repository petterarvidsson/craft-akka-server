package konstructs.forest;

import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;

import konstructs.plugin.KonstructsActor;
import konstructs.api.*;

class Forest extends KonstructsActor {
    static final BlockTypeId SAPLING_ID = new BlockTypeId("org/konstructs/forest", "sapling");
    static final BlockTypeId WOOD_ID = new BlockTypeId("org/konstructs", "wood");
    static final BlockTypeId LEAVES_ID = new BlockTypeId("org/konstructs", "leaves");
    static final BlockTypeId DIRT_GRASS_ID = new BlockTypeId("org/konstructs", "grass-dirt");
    static final BlockTypeId VACUUM_ID = new BlockTypeId("org/konstructs", "vacuum");
    static final Block SAPLING = Block.create(SAPLING_ID);

    static final int SEED_HEIGHT_DIFFERANCE = 5;
    static final int MAX_SEED_HEIGHT = 48;
    static final int MIN_SEED_HEIGHT = 5;

    public Forest(ActorRef universe, Position start) {
        super(universe);
        getSelf().tell(new SeedTree(start), getSelf());
        System.out.println("I'm a forest!");
    }

    void tryToSeed(Position pos) {
        Position start =
            new Position(pos.x(),
                         Math.max(pos.y() - SEED_HEIGHT_DIFFERANCE, MIN_SEED_HEIGHT),
                         pos.z());
        Position end =
            new Position(pos.x() + 1,
                         Math.min(pos.y() + SEED_HEIGHT_DIFFERANCE, MAX_SEED_HEIGHT),
                         pos.z() + 1);
        boxQuery(start, end);
    }

    void seed(Position pos) {
        System.out.println("Seeding a new one at: " + pos);
        putBlock(pos, SAPLING);
        getContext().actorOf(CanATreeGrowHere.props(getUniverse(), pos));
    }

    void plant(Position pos) {
        System.out.println("Plant tree!");
        getContext().actorOf(Tree.props(getUniverse(), pos, 5));
    }

    @Override
    public void onBoxQueryResult(BoxQueryResult result) {
        for(Map.Entry<Position, BlockTypeId> p: result.result().toPlaced().entrySet()) {
            if(p.getValue().equals(DIRT_GRASS_ID)) {
                Position pos = p.getKey();
                seed(new Position(pos.x(), pos.y() + 1, pos.z()));
                return;
            }
        }
        System.out.println("No suitable position found :-(");
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof SeedTree) {
            SeedTree seedTree = (SeedTree)message;
            seed(seedTree.getPosition());
        } else if(message instanceof PlantTree) {
            PlantTree plantTree = (PlantTree)message;
            plant(plantTree.getPosition());
        } else if(message instanceof TryToSeedTree) {
            TryToSeedTree tryToSeedTree = (TryToSeedTree)message;
            tryToSeed(tryToSeedTree.getPosition());
            System.out.println("Seed a new one!");
        } else {
            super.onReceive(message); // Handle konstructs messages
        }
    }

    public static Props props(ActorRef universe, Position start) {
        return Props.create(Forest.class, universe, start);
    }

}
