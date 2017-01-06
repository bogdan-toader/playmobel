package lu.mobilab.playmobel.backendv2.util;

import org.mwg.ml.algorithm.profiling.ProbaDistribution;
import org.mwg.ml.common.matrix.VolatileDMatrix;
import org.mwg.ml.common.matrix.operation.MultivariateNormalDistribution;
import org.mwg.struct.DMatrix;

import java.util.ArrayList;

/**
 * Created by assaa on 15/12/2016.
 */
public class GMMJava {
    private double[] min;
    private double[] max;
    private double[] sum;
    private double[] sumsq;
    private int total;
    private int level;
    private GMMConfig rootconfig;
    private ArrayList<GMMJava> subGaussians;


    public GMMJava(GMMConfig config) {
        if (config != null) {
            this.rootconfig = config;
            this.level = config.maxLevels;
        }
    }

    private GMMJava createLevel(double[] values, final int level, final int width, final double compressionFactor, final int compressionIter, final double[] precisions, final double threshold) {
        GMMJava g = new GMMJava(null);
        g.level = level;
        g.internallearn(values, width, compressionFactor, compressionIter, precisions, threshold, false); //dirac
        if (this.subGaussians == null) {
            this.subGaussians = new ArrayList<>();
        }
        this.subGaussians.add(g);
        return g;
    }


    public static double distance(double[] features, double[] avg, double[] precisions) {
        double max = 0;
        double temp;
        for (int i = 0; i < features.length; i++) {
            temp = (features[i] - avg[i]) * (features[i] - avg[i]) / precisions[i];
            if (temp > max) {
                max = temp;
            }
        }
        return Math.sqrt(max);
    }


    private void internallearn(final double[] values, final int width, final double compressionFactor, final int compressionIter, final double[] precisions, double threshold, final boolean createNode) {
        if (total == 0) {
            sum = new double[values.length];
            System.arraycopy(values, 0, sum, 0, values.length);
            total = 1;
        } else {
            int dim = sum.length;
            boolean reccursive = false;
            if (values.length != dim) {
                throw new RuntimeException("values should always have the same dimensions");
            }
            if (total == 1) {
                min = new double[dim];
                max = new double[dim];
                sumsq = new double[dim * (dim + 1) / 2];
                System.arraycopy(sum, 0, min, 0, dim);
                System.arraycopy(sum, 0, max, 0, dim);
                int count = 0;
                for (int i = 0; i < dim; i++) {
                    for (int j = i; j < dim; j++) {
                        sumsq[count] = sum[i] * sum[j];
                        count++;
                    }
                }

                if (createNode && level > 0) {
                    GMMJava newLev = createLevel(sum, level - 1, width, compressionFactor, compressionIter, precisions, threshold);
                    double d = distance(values, sum, precisions);
                    if (d < threshold) {
                        reccursive = true;
                        newLev.internallearn(values, width, compressionFactor, compressionIter, precisions, threshold, createNode);
                    }
                }
            }
            //Update the values
            for (int i = 0; i < dim; i++) {
                if (values[i] < min[i]) {
                    min[i] = values[i];
                }
                if (values[i] > max[i]) {
                    max[i] = values[i];
                }
                sum[i] += values[i];
            }

            int count = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    sumsq[count] += values[i] * values[j];
                    count++;
                }
            }
            total++;
            if (createNode && level > 0 && !reccursive) {
                createLevel(values, level - 1, width, compressionFactor, compressionIter, precisions, threshold);
                checkAndCompress(width, compressionFactor, compressionIter, precisions, threshold);
            }
        }
    }

    public double[] getAvg() {
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return sum.clone();
        } else {
            double[] avg = sum.clone();
            for (int i = 0; i < avg.length; i++) {
                avg[i] = avg[i] / total;
            }
            return avg;
        }

    }


    private void updateLevel(final int newLevel) {
        this.level = newLevel;
        if (this.subGaussians != null) {
            if (newLevel == 0) {
                this.subGaussians.clear();
            } else {
                for (int i = 0; i < this.subGaussians.size(); i++) {
                    this.subGaussians.get(i).updateLevel(newLevel - 1);
                }
            }
        }
    }

    private void move(GMMJava subgaus) {
        //Start the merging phase


        double[] sum2 = subgaus.sum;
        double[] min2 = subgaus.getMin();
        double[] max2 = subgaus.getMax();
        double[] sumsquares2 = subgaus.getSumSquares();

        int dim = sum.length;
        if (total == 1) {
            min = new double[dim];
            max = new double[dim];
            sumsq = new double[dim * (dim + 1) / 2];
            System.arraycopy(sum, 0, min, 0, dim);
            System.arraycopy(sum, 0, max, 0, dim);
            int count = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    sumsq[count] = sum[i] * sum[j];
                    count++;
                }
            }
        }


        for (int i = 0; i < sum.length; i++) {
            sum[i] = sum[i] + sum2[i];
            if (min2[i] < min[i]) {
                min[i] = min2[i];
            }
            if (max2[i] > max[i]) {
                max[i] = max2[i];
            }
        }

        for (int i = 0; i < sumsq.length; i++) {
            sumsq[i] = sumsq[i] + sumsquares2[i];
        }

        total = total + subgaus.total;

        //Add the subGaussians to the relationship
        if (level > 0) {
            ArrayList<GMMJava> subrelations = subgaus.subGaussians;
            if (subrelations == null || subrelations.size() == 0) {
                subgaus.updateLevel(level - 1);
                this.subGaussians.add(subgaus);
            } else {
                for (int i = 0; i < subrelations.size(); i++) {
                    this.subGaussians.add(subrelations.get(i));
                }
            }
        }
    }


    private void checkAndCompress(final int width, final double compressionFactor, final int compressionIter, final double[] precisions, final double threshold) {
        final GMMJava selfPointer = this;

        if (subGaussians != null && subGaussians.size() >= compressionFactor * width) {
            double[][] data = new double[subGaussians.size()][];
            for (int i = 0; i < subGaussians.size(); i++) {
                data[i] = subGaussians.get(i).getAvg();
            }
            //Cluster the different gaussians
            KMeans clusteringEngine = new KMeans();
            int[][] clusters = clusteringEngine.getClusterIds(data, width, compressionIter, precisions);

            //Select the ones which will remain as head by the maximum weight
            GMMJava[] mainClusters = new GMMJava[width];
            for (int i = 0; i < width; i++) {
                if (clusters[i] != null && clusters[i].length > 0) {
                    int max = 0;
                    int maxpos = 0;
                    for (int j = 0; j < clusters[i].length; j++) {
                        int x = subGaussians.get(clusters[i][j]).total;
                        if (x > max) {
                            max = x;
                            maxpos = clusters[i][j];
                        }
                    }
                    mainClusters[i] = subGaussians.get(maxpos);
                }
            }

            ArrayList<GMMJava> subGaussiansCopy = new ArrayList<>(subGaussians.size());
            for (int k = 0; k < subGaussians.size(); k++) {
                subGaussiansCopy.add(subGaussians.get(k));
            }

            //move the nodes
            for (int i = 0; i < width; i++) {
                //if the main cluster node contains only 1 sample, it needs to clone itself in itself
                if (clusters[i].length > 1 && mainClusters[i].total == 1 && mainClusters[i].level > 0) {
                    mainClusters[i].createLevel(mainClusters[i].getAvg(), mainClusters[i].level - 1, width, compressionFactor, compressionIter, precisions, threshold);
                }

                if (clusters[i] != null && clusters[i].length > 0) {
                    for (int j = 0; j < clusters[i].length; j++) {
                        GMMJava g = subGaussiansCopy.get(clusters[i][j]);
                        if (g != mainClusters[i]) {
                            mainClusters[i].move(g);
                            selfPointer.subGaussians.remove(g);
                        }
                    }
                    mainClusters[i].checkAndCompress(width, compressionFactor, compressionIter, precisions, threshold);
                }
            }
        }
    }

    private double[] getCovarianceArray(double[] avg, double[] err) {
        if (avg == null) {
            double[] errClone = new double[err.length];
            System.arraycopy(err, 0, errClone, 0, err.length);
            return errClone;
        }
        if (err == null) {
            err = new double[avg.length];
        }
        int features = avg.length;

        if (total == 0) {
            return null;
        }
        if (total > 1) {
            double[] covariances = new double[features];

            double correction = total;
            correction = correction / (total - 1);

            int count = 0;
            for (int i = 0; i < features; i++) {
                covariances[i] = (sumsq[count] / total - avg[i] * avg[i]) * correction;
                if (covariances[i] < err[i]) {
                    covariances[i] = err[i];
                }
                count += features - i;
            }
            return covariances;
        } else {
            return err;
        }
    }

    private GMMJava filter(final ArrayList<GMMJava> result, final double[] features, final double[] precisions, double threshold, double level) {
        double threshold2 = threshold + level * 0.707;
        if (result == null || result.size() == 0) {
            return null;
        }
        double[] distances = new double[result.size()];
        double min = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < result.size(); i++) {
            double[] avg = result.get(i).getAvg();
            distances[i] = distance(features, avg, result.get(i).getCovarianceArray(avg, precisions));
            if (distances[i] < min) {
                min = distances[i];
                index = i;
            }
        }
        if (min < threshold2) {
            return result.get(index);
        } else {
            return null;
        }
    }


    public boolean totalCheck(){
        ArrayList<GMMJava> toCheck=new ArrayList<>();
        toCheck.add(this);
        boolean result=true;
        int totaltemp;
        while (toCheck.size()>0) {
            ArrayList<GMMJava> toCheckNext = new ArrayList<>();
            for(int i=0;i<toCheck.size();i++){
                GMMJava temp= toCheck.get(i);
                if(temp.subGaussians!=null && temp.subGaussians.size()>0){
                    totaltemp=0;
                    for(int j=0;j<temp.subGaussians.size();j++){
                        totaltemp+=temp.subGaussians.get(j).total;
                        toCheckNext.add(temp.subGaussians.get(j));
                    }
                    if(totaltemp!=temp.total){
                        System.out.println("Error found");
                        result=false;
                    }
                }
            }
            toCheck=toCheckNext;
        }



        return result;
    }


    public ProbaDistribution generateDistributions(int reqlevel) {

        if (sum == null || sum.length==0) {
            return null;
        }
        final int dim = sum.length;
        final double[] err = rootconfig.resolution;

        ArrayList<GMMJava> current = new ArrayList<>();



        current.add(this);

        ArrayList<GMMJava> keep = new ArrayList<>();
        for (int k = 0; k < this.level - reqlevel; k++) {
            ArrayList<GMMJava> traverse = new ArrayList<>();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).subGaussians != null && current.get(i).subGaussians.size() > 0) {
                    for (int j = 0; j < current.get(i).subGaussians.size(); j++) {
                        traverse.add(current.get(i).subGaussians.get(j));
                    }
                } else {
                    keep.add(current.get(i));
                }
            }
            current = traverse;
            if (current.size() == 0) {
                break;
            }
        }

        if(current.size()>0){
            for(int i=0;i<current.size();i++){
                keep.add(current.get(i));
            }
        }


        DMatrix covBackup = VolatileDMatrix.empty(dim,dim);
        for (int i = 0; i < dim; i++) {
            covBackup.set(i, i, err[i]);
        }

        MultivariateNormalDistribution mvnBackup = new MultivariateNormalDistribution(null, covBackup, false);

        int[] totals = new int[keep.size()];
        int globalTotal = 0;

        MultivariateNormalDistribution[] distributions = new MultivariateNormalDistribution[keep.size()];
        for (int i = 0; i < keep.size(); i++) {
            GMMJava temp = keep.get(i);
            totals[i] = temp.total;
            globalTotal += totals[i];
            double[] avg = temp.getAvg();
            if (totals[i] > 2) {
                distributions[i] = new MultivariateNormalDistribution(avg, temp.getCovariance(avg, err), false);
                distributions[i].setMin(temp.getMin());
                distributions[i].setMax(temp.getMax());
            } else {
                distributions[i] = mvnBackup.clone(avg); //this can be optimized later by inverting covBackup only once
            }
        }
        return new ProbaDistribution(totals, distributions, globalTotal);


    }

    private DMatrix getCovariance(double[] avg, double[] err) {
        int features = avg.length;

        if (total == 0) {
            return null;
        }
        if (err == null) {
            err = new double[avg.length];
        }
        if (total > 1) {
            double[] covariances = new double[features * features];

            double correction = total;
            correction = correction / (total - 1);

            int count = 0;
            for (int i = 0; i < features; i++) {
                for (int j = i; j < features; j++) {
                    covariances[i * features + j] = (sumsq[count] / total - avg[i] * avg[j]) * correction;
                    covariances[j * features + i] = covariances[i * features + j];
                    count++;
                    if (covariances[i * features + i] < err[i]) {
                        covariances[i * features + i] = err[i];
                    }
                }
            }
            return VolatileDMatrix.wrap(covariances, features, features);
        } else {
            return null;
        }
    }


    public void learnVector(double[] values) {
        GMMJava result = this;

        while (result != null) {
            GMMJava parent = result;
            result = filter(result.subGaussians, values, rootconfig.resolution, rootconfig.threshold, parent.level - 1.0);
            if (result != null) {
                parent.internallearn(values, rootconfig.width, rootconfig.compressionFactor, rootconfig.compressionIter, rootconfig.resolution, rootconfig.threshold, false);
            } else {
                parent.internallearn(values, rootconfig.width, rootconfig.compressionFactor, rootconfig.compressionIter, rootconfig.resolution, rootconfig.threshold, true);
            }
        }
    }


    public double[] getSumSquares() {
        if (sumsq != null) {
            return sumsq;
        } else if (sum != null) {
            int dim = sum.length;
            double[] res = new double[dim * (dim + 1) / 2];
            int count = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    res[count] = sum[i] * sum[j];
                    count++;
                }
            }
            return res;
        }
        return null;
    }

    public double[] getMax() {
        if (max != null) {
            return max;
        } else {
            return sum;
        }
    }

    public double[] getMin() {
        if (min != null) {
            return min;
        } else {
            return sum;
        }
    }
}
