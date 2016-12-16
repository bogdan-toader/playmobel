package lu.mobilab.playmobel.backendv2.util;

import org.mwg.ml.algorithm.profiling.ProbaDistribution;

import java.util.Random;

/**
 * Created by assaa on 15/12/2016.
 */
public class GMMJavaTest {

    public static void main(String arg[]) {
        int dim=2;
        int test=1000000;

        GMMConfig conf = new GMMConfig();
        conf.maxLevels = 2;
        conf.width=5;
        conf.compressionFactor=1;
        conf.compressionIter=10;
        conf.threshold=0.00001;
        conf.resolution=new double[dim];
        for(int i=0;i<dim;i++){
            conf.resolution[i]=0.00001;
        }
        GMMJava gmm = new GMMJava(conf);

        Random random=new Random(123);
        double[] var=new double[dim];

        long starttime=System.currentTimeMillis();
        for(int i=0;i<test;i++){
            for(int j=0; j<dim;j++){
                var[j]=random.nextDouble();
            }
            gmm.learnVector(var);
            if(i%10000==0){
                long endtime=System.currentTimeMillis();
                double elapsed=endtime-starttime;
                elapsed=elapsed/1000;
                double speed=i/elapsed;
                System.out.println("done: "+i+" speed: "+speed+" values/s");
            }
        }

        long endtime=System.currentTimeMillis();
        double elapsed=endtime-starttime;
        elapsed=elapsed/1000;
        double speed=test/elapsed;
        System.out.println("done: "+test+" speed: "+speed+" values/s");


        starttime=System.currentTimeMillis();
        ProbaDistribution pb=gmm.generateDistributions(3);
        endtime=System.currentTimeMillis();
        elapsed=endtime-starttime;

        gmm. totalCheck();
        System.out.println("Generated proba in "+elapsed+" ms with "+pb.distributions.length+" components! total: " +pb.global );


        int s=0;




    }

}
