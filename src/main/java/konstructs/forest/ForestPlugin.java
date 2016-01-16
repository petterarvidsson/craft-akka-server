package konstructs.forest;

import akka.actor.ActorRef;
import akka.actor.Props;
import konstructs.plugin.KonstructsActor;
import konstructs.plugin.PluginConstructor;
import konstructs.api.Position;

public class ForestPlugin extends KonstructsActor {
    public ForestPlugin(String name, ActorRef universe) {
        super(universe);
        getContext().actorOf(Forest.props(universe, new Position(-532, 25, -560)));
    }

    @PluginConstructor
    public static Props props(String pluginName, ActorRef universe) {
        Class currentClass = new Object() { }.getClass().getEnclosingClass();
        return Props.create(currentClass, pluginName, universe);
    }
}
