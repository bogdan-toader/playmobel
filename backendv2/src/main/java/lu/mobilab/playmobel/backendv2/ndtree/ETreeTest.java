package lu.mobilab.playmobel.backendv2.ndtree;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Type;
import org.mwg.structure.StructurePlugin;
import org.mwg.structure.tree.ETree;

import java.util.Random;

public class ETreeTest {

    public static void main(String[] arg) {
        int dim = 5;
        double[] precisions = new double[dim];
        double[] boundMin = new double[dim];
        double[] boundMax = new double[dim];
        for (int i = 0; i < dim; i++) {
            precisions[i] = 0.001;
            boundMin[i] = 0;
            boundMax[i] = 1;
        }

        NDTreeConfig config=new NDTreeConfig(boundMin,boundMax,precisions,0);
        NDTree eTree = new NDTree(config);

        Random random = new Random();
        random.setSeed(125362l);
        int ins = 10_000_000;


        double[][] keys = new double[ins][];
        for (int i = 0; i < ins; i++) {
            //temp.setProperty("value", Type.DOUBLE, random.nextDouble());
            double[] key = new double[dim];
            for (int j = 0; j < dim; j++) {
                key[j] = random.nextDouble();
            }
            keys[i] = key;
        }
        long ts = System.currentTimeMillis();
        for (int i = 0; i < ins; i++) {
            eTree.insert(keys[i]);
        }
        long te=System.currentTimeMillis()-ts;
        eTree.print();

        System.out.println(te);

    }

}
