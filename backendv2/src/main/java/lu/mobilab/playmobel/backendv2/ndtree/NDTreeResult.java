package lu.mobilab.playmobel.backendv2.ndtree;


import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by assaad on 18/12/2016.
 */
public class NDTreeResult {
    public class AtomicRes {
        private double[] val;
        private int tot;

        AtomicRes(double[] values, int total) {
            val = values;
            tot = total;
        }

        public double[] getVal() {
            return val;
        }

        public int getTot() {
            return tot;
        }
    }


    private ArrayList<AtomicRes> res;
    private int global = 0;
    private GaussianProfile profile;
    private NDTreeConfig config;

    public GaussianProfile getProfile() {
        return profile;
    }

    public NDTreeResult(NDTreeConfig config) {
        this.config = config;
        this.profile = new GaussianProfile();
        this.res = new ArrayList<>();
    }

    public void add(double[] values, int total) {
        this.res.add(new AtomicRes(values, total));
        profile.learn(values);
        global += total;
    }


    public ArrayList<AtomicRes> getResult() {
        return res;
    }

    public int getGlobal() {
        return global;
    }

    public NDTreeResult sort() {
        res.sort(new Comparator<AtomicRes>() {
            @Override
            public int compare(AtomicRes o1, AtomicRes o2) {
                return Integer.compare(o2.getTot(), o1.getTot());
            }
        });
        return this;
    }

    public NDTreeResult sortAndDisplay(int num) {
        sort();
        int disp = Math.min(num, res.size());

        for (int i = 0; i < disp; i++) {
            System.out.println(GaussianProfile.printArray((i + 1) + " " + res.get(i).getTot(), res.get(i).getVal()));
        }
        System.out.println("Result profile:");
        profile.print();
        return this;
    }


    //"*" is to group by this dimension
    //"-" is to keep the precision intact
    //any other number is to group by the new precision
    public NDTreeResult groupBy(String[] groupby) {
        long ts = System.nanoTime();
        if (res.size() == 0) {
            return this;
        }

        int dim = this.config.getMin().length;
        boolean[] keep = new boolean[dim];
        int newdim = 0;
        for (int i = 0; i < dim; i++) {
            if (groupby[i].equals("*")) {
                keep[i] = false;
            } else {
                keep[i] = true;
                newdim++;
            }
        }
        if (newdim == 0) {
            NDTreeResult res = new NDTreeResult(null);
            res.add(new double[0], this.getGlobal());
            return res;
        }
        double[] minprofile = profile.getMin();
        double[] maxprofile = profile.getMax();
        double[] resolution = config.getResolution();

        double[] newmin = new double[newdim];
        double[] newmax = new double[newdim];
        double[] newresolution = new double[newdim];
        String[] newresStr = new String[newdim];

        int c = 0;
        for (int i = 0; i < dim; i++) {
            if (keep[i]) {
                newmin[c] = minprofile[i];
                newmax[c] = maxprofile[i];
                newresStr[c] = groupby[i];
                if (groupby[i].equals("-")) {
                    newresolution[c] = resolution[i];
                    newresStr[c]= String.valueOf(newresolution[c]);
                } else {
                    newresolution[c] = Double.parseDouble(groupby[i]);
                }
                if (newmax[c] == newmin[c]) {
                    newmax[c] += newresolution[c];
                }
                c++;
            }
        }
        int[][] fractions = convertToDenum(newresStr);

        //Convert newmin, newmax
        for (int i = 0; i < newdim; i++) {
            newmin[i] = (int) ((newmin[i] - 1.5 * newresolution[i]) * fractions[i][1]);
            newmin[i] = (int) (newmin[i] / fractions[i][0]);
            newmin[i] = newmin[i] * fractions[i][0] / fractions[i][1];

            newmax[i] = (int) ((newmax[i] + 1.5 * newresolution[i]) * fractions[i][1]);
            newmax[i] = (int) (newmax[i] / fractions[i][0]);
            newmax[i] = newmax[i] * fractions[i][0] / fractions[i][1];
        }

        NDTreeConfig newconfig = new NDTreeConfig(newmin, newmax, newresolution, 0);
        NDTree groupedtree = new NDTree(newconfig);

        for (AtomicRes toinsert : res) {
            //todo to optimize by allowing the tree to take weights!
            double[] conv = convert(toinsert.getVal(), keep, newdim, fractions);
            for (int i = 0; i < toinsert.getTot(); i++) {
                groupedtree.insert(conv);
            }
        }
        groupedtree.print();
//        System.out.println();
        NDTree.reportTime("grouped", ts);
        return groupedtree.filter(newmin, newmax);
    }

    private static int[][] convertToDenum(String[] res) {
        int[][] result = new int[res.length][];
        for (int i = 0; i < res.length; i++) {
            result[i] = new int[2];
            if (res[i].contains(".")) {
                int digitsDec = res[i].length() - 1 - res[i].indexOf('.');
                double d = Double.valueOf(res[i]);
                result[i][1] = 1;
                for (int j = 0; j < digitsDec; j++) {
                    d *= 10;
                    result[i][1] = result[i][1] * 10;
                }
                result[i][0] = (int) d;

            } else {
                result[i][0] = Integer.valueOf(res[i]);
                result[i][1] = 1;
            }

            if (result[i][0] % result[i][1] == 0) {
                result[i][0] = result[i][0] / result[i][1];
                result[i][1] = 1;
            }

        }

        return result;
    }


    private static double[] convert(final double[] val, final boolean[] keep, final int dim, int[][] fractions) {
        double[] temp = new double[dim];
        int c = 0;
        for (int i = 0; i < val.length; i++) {
            if (keep[i]) {
                temp[c] = val[i];
//                temp[c] = (int) (val[i] * fractions[c][1]);
//                temp[c] = (int) (temp[c] / fractions[c][0]);
//                temp[c] = temp[c] * fractions[c][0] / fractions[c][1];
                c++;
            }
        }
        return temp;
    }

}
