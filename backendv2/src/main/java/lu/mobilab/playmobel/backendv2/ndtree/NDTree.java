package lu.mobilab.playmobel.backendv2.ndtree;

import java.util.ArrayList;

public class NDTree {
    private double[] _min;
    private double[] _max;
    private int _total = 0;
    private NDTree[] _subchildren;
    private ArrayList<double[]> _tempValues;
    private GaussianProfile lowlevel;
    private NDTreeConfig _config;


    private NDTree(final double[] min, final double[] max, final double[] center, final double[] keyToInsert) {
        this._min = new double[min.length];
        this._max = new double[max.length];
        for (int i = 0; i < min.length; i++) {
            if (keyToInsert[i] <= center[i]) {
                this._min[i] = min[i];
                this._max[i] = center[i];

            } else {
                this._min[i] = center[i];
                this._max[i] = max[i];
            }
        }

        this._tempValues = new ArrayList<double[]>();
    }

    public NDTree(NDTreeConfig config) {
        this._min = config.getMin();
        this._max = config.getMax();
        this._config = config;
        this._tempValues = new ArrayList<double[]>();
    }

    private void check(double[] values) {
        if (values.length != _min.length) {
            throw new RuntimeException("Values dimension mismatch");
        }
        for (int i = 0; i < _min.length; i++) {
            if (values[i] < _min[i] || values[i] > _max[i]) {
                throw new RuntimeException("Values should be between min, max, " + GaussianProfile.printArray("ERROR:", values));
            }
        }
    }

    private static int getChildren(int dim) {
        int total = 1;
        for (int i = 0; i < dim; i++) {
            total = total << 1;
        }
        return total;
    }

    private static int getRelationId(double[] centerKey, double[] keyToInsert) {
        int result = 0;
        for (int i = 0; i < centerKey.length; i++) {
            if (i != 0) {
                result = result << 1;
            }
            if (keyToInsert[i] > centerKey[i]) {
                result += 1;
            }
        }
        return result;
    }

    private static boolean checkCreateLevels(double[] min, double[] max, double[] resolutions) {
        for (int i = 0; i < min.length; i++) {
            if ((max[i] - min[i]) > resolutions[i]) {
                return true;
            }
        }
        return false;
    }

    private double[] getCenter() {
        double[] center = new double[_min.length];
        for (int i = 0; i < _min.length; i++) {
            center[i] = (_max[i] + _min[i]) / 2;
        }
        return center;
    }

    public void insert(final double[] values) {
        check(values);
        _config.updateStat(values);
        internalInsert(values, _min, _max, getCenter(), _config.getResolution(), _config.getMaxPerLevel(), 0, this);
    }


    private void internalInsert(final double[] values, final double[] min, final double[] max, final double[] center, final double[] resolution, final int maxPerLevel, final int lev, final NDTree root) {
        //check if it has subchildrens
        if (this != root) {
            root._config.incJumpCounter();
        }

        if (_subchildren != null) {
            int index = getRelationId(center, values);
            if (_subchildren[index] == null) {
                _subchildren[index] = new NDTree(min, max, center, values);
            }
            _subchildren[index].internalInsert(values, _subchildren[index]._min, _subchildren[index]._max, _subchildren[index].getCenter(), resolution, maxPerLevel, lev + 1, root);
        } else if (_tempValues != null) {
            // check if we can create subchildren
            if (checkCreateLevels(min, max, resolution)) {
                if (_tempValues.size() < maxPerLevel) {
                    _tempValues.add(values);
                } else {
                    _subchildren = new NDTree[getChildren(min.length)];
                    _tempValues.add(values);
                    for (int i = 0; i < _tempValues.size(); i++) {
                        final double[] toInsert = _tempValues.get(i);
                        int index = getRelationId(center, toInsert);
                        if (_subchildren[index] == null) {
                            _subchildren[index] = new NDTree(min, max, center, toInsert);
                            root._config.incCreatedNode();
                        }
                        _subchildren[index].internalInsert(toInsert, _subchildren[index]._min, _subchildren[index]._max, _subchildren[index].getCenter(), resolution, maxPerLevel, lev + 1, root);
                    }
                    _tempValues = null;
                }
            }
            //Else we reached here last level of the tree, and the array is full, we need to start a profiler
            else {
                if (lowlevel == null) {
                    _tempValues = null;
                    lowlevel = new GaussianProfile();
                }
                lowlevel.learn(values);
            }

        }
        //this is for everyone
        root._config.updateMaxLev(lev);
        _total++;
    }

    public int size() {
        return _total;
    }


    //"*" is to group by this dimension
    //"-" is to keep the precision intact
    //any other number is to group by the new precision
    public NDTreeResult filter(double[] min, double[] max, String[] groupby) {
        long ts = System.nanoTime();
        final NDTreeResult result = new NDTreeResult();
        if (checkbound(_min, _max, min, max)) {
            internalFilter(min, max, result);
        }
        long te = System.nanoTime() - ts;
        if (te < 1000000) {
            System.out.println("filtered in: " + te + " ns!");
        } else {
            te = te / 1000000;
            System.out.println("filtered in: " + te + " ms!");
        }
        if (groupby != null && groupby.length == min.length) {
            boolean[] keep = new boolean[min.length];
            int newdim = 0;
            for (int i = 0; i < min.length; i++) {
                if (groupby[i].equals("*")) {
                    keep[i] = false;
                } else {
                    keep[i] = true;
                    newdim++;
                }
            }
            if (newdim == 0) {
                NDTreeResult res = new NDTreeResult();
                res.add(new double[0], result.getGlobal());
                return res;
            }
            double[] minprofile = result.getProfile().getMin();
            double[] maxprofile = result.getProfile().getMax();
            double[] resolution = _config.getResolution();

            double[] newmin = new double[newdim];
            double[] newmax = new double[newdim];
            double[] newresolution = new double[newdim];

            int c = 0;
            for (int i = 0; i < min.length; i++) {
                if (keep[i]) {
                    newmin[c] = minprofile[i];
                    newmax[c] = maxprofile[i];
                    if (groupby[i].equals("-")) {
                        newresolution[c] = resolution[i];
                    } else {
                        newresolution[c] = Double.parseDouble(groupby[i]);
                    }
                    if (newmax[c] == newmin[c]) {
                        newmax[c] += newresolution[c];
                    }
                    c++;
                }
            }

            NDTreeConfig config = new NDTreeConfig(newmin, newmax, newresolution, 0);


            return null;
        } else {
            return result;
        }
    }

    private void internalFilter(final double[] requestedmin, final double[] requestedmax, final NDTreeResult result) {
        if (_subchildren == null) {
            if (_tempValues != null) {
                for (double[] val : _tempValues) {
                    if (checkInside(val, requestedmin, requestedmax)) {
                        result.add(val, 1);
                    }
                }
            } else {
                result.add(lowlevel.getAvg(), _total);
            }
        } else {
            for (NDTree subt : _subchildren) {
                if (subt != null && checkbound(subt._min, subt._max, requestedmin, requestedmax)) {
                    subt.internalFilter(requestedmin, requestedmax, result);
                }
            }

        }
    }

    private static boolean checkbound(double[] min, double[] max, double[] requestedmin, double[] requestedmax) {
        for (int i = 0; i < min.length; i++) {
            if (min[i] > requestedmax[i] || max[i] < requestedmin[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkInside(double[] val, double[] requestedmin, double[] requestedmax) {
        for (int i = 0; i < val.length; i++) {
            if (val[i] < requestedmin[i] || val[i] > requestedmax[i]) {
                return false;
            }
        }
        return true;
    }


    public void print() {
        System.out.println("TREE size: " + size());
        System.out.println("SUBTREES: " + _config.getCreatedNodes());
        System.out.println("MAXDEPTH: " + _config.getMaxlev());
        System.out.println("JUMPS: " + _config.getJumpcounter());
        _config.getProfile().print();
    }


}
