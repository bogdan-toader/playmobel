package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.ml.MLPlugin;
import org.mwg.structure.NTree;
import org.mwg.structure.StructurePlugin;
import org.mwg.structure.tree.NDTree;

/**
 * Created by assaad on 19/12/2016.
 */
public class Test {
    public static void main(String[] arg){
        Graph graph= GraphBuilder
                .newBuilder()
                .withPlugin(new MLPlugin())
                .withPlugin(new StructurePlugin())
                .withStorage(new LevelDBStorage("/ptath"))
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                NTree tree = (NTree) graph.newTypedNode(0,0, NDTree.NAME);

                tree.setAt(NDTree.BOUND_MIN, Type.DOUBLE_ARRAY, new double[]{-90,-180});
                tree.setAt(NDTree.BOUND_MAX, Type.DOUBLE_ARRAY, new double[]{90,180});
                tree.setAt(NDTree.RESOLUTION, Type.DOUBLE_ARRAY, new double[]{0.0004,0.0008});

                Node user=graph.newNode(0,0);
                tree.insertWith(new double[]{44, 6}, user, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        System.out.println();
                    }
                });

                tree.nearestN(new double[]{44, 6}, 100, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {

                    }
                });

            }
        });



    }
}
