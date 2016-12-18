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

    public GaussianProfile getProfile(){
        return profile;
    }

    public NDTreeResult() {
        this.profile=new GaussianProfile();
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

    public void sortAndDisplay(int num) {
        res.sort(new Comparator<AtomicRes>() {
            @Override
            public int compare(AtomicRes o1, AtomicRes o2) {
                return Integer.compare(o2.getTot(), o1.getTot());
            }
        });

        int disp = Math.min(num, res.size());

        for (int i = 0; i < disp; i++) {
            System.out.println(GaussianProfile.printArray((i + 1) + "- " + res.get(i).getTot() + ": ", res.get(i).getVal()));
        }
        System.out.println("Result profile:");
        profile.print();
    }


}
