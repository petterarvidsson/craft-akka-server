package konstructs.forest;

import akka.actor.ActorRef;
import akka.actor.Props;

import konstructs.plugin.KonstructsActor;
import konstructs.api.*;

class CanATreeGrowHere extends KonstructsActor {
    static final int TRUNK_RADI = 2;
    static final int TRUNK_HEIGHT = 5;
    static final int CROWN_RADI = 10;
    static final int CROWN_HEIGHT = 20;

    private Position position;
    private boolean trunkIsOk = false;

    public CanATreeGrowHere(ActorRef universe, Position position) {
        super(universe);
        this.position = position;
        System.out.println("Check for somewhere to grow!");
        queryForTrunk();
    }

    private void query(int radi, int height, int yOffset) {
        Position start =
            new Position(position.x() - radi,
                         position.y() + yOffset,
                         position.z() - radi);
        Position end =
            new Position(position.x() + radi,
                         position.y() + height + yOffset,
                         position.z() + radi);
        boxQuery(start, end);
    }

    private void queryForTrunk() {
        query(TRUNK_RADI, TRUNK_HEIGHT, 1);
    }

    private void queryForCrown() {
        query(CROWN_RADI, CROWN_HEIGHT, TRUNK_HEIGHT);
    }

    private void canGrow() {
        getContext().parent().tell(new PlantTree(position), getSelf());
        System.out.println("Can grow here!");
        getContext().stop(getSelf()); /* We are done, let's die*/
    }

    private void canNotGrow() {
        System.out.println("Can not grow here!");
        getContext().stop(getSelf()); /* We are done, let's die*/
    }

    @Override
    public void onBoxQueryResult(BoxQueryResult result) {
        for(Placed<BlockTypeId> p: result.result().toPlaced()) {
            if(!(p.block().equals(Forest.VACUUM_ID) || p.block().equals(Forest.LEAVES_ID))) {
                canNotGrow();
                return;
            }
        }
        if(trunkIsOk) {
            canGrow();
        } else {
            trunkIsOk = true;
            queryForCrown();
        }
    }

    public static Props props(ActorRef universe, Position start) {
        return Props.create(CanATreeGrowHere.class, universe, start);
    }
}
