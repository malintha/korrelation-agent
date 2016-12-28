package m8.agents;

public class Main {

    public static double getPartialCorrelationControlledZ(double rxy, double rxz, double ryz) {
        double rxy_z = 0;
        rxy_z = (rxy - rxz * ryz) / (Math.sqrt(1 - rxz * rxz) * Math.sqrt(1 - ryz * ryz));
        return rxy_z;
    }

    public static double getCorrelationXZ(double[] x, double[] y) {
        double sumx = 0.0, sumy = 0.0;
        int n = x.length;
        for (int i = 0; i < x.length; i++) {
            sumx += x[i];
            sumy += y[i];
        }

        double xbar = sumx / n;
        double ybar = sumy / n;

        double[] xi_xbar = new double[n];
        double[] yi_ybar = new double[n];
        double[] xi_xbar_yi_ybar = new double[n];
        double[] xi_xbar_sq = new double[n];
        double[] yi_ybar_sq = new double[n];

        for (int i = 0; i < n; i++) {
            xi_xbar[i] = x[i] - xbar;
            yi_ybar[i] = y[i] - ybar;
            xi_xbar_yi_ybar[i] = xi_xbar[i] * yi_ybar[i];
            xi_xbar_sq[i] = xi_xbar[i] * xi_xbar[i];
            yi_ybar_sq[i] = yi_ybar[i] * yi_ybar[i];
        }

        double sig_xi_xbar_sq = 0, sig_yi_ybar_sq = 0, sig_xi_xbar_yi_ybar = 0;

        for (int i = 0; i < n; i++) {
            sig_xi_xbar_sq += xi_xbar_sq[i];
            sig_yi_ybar_sq += yi_ybar_sq[i];
            sig_xi_xbar_yi_ybar += xi_xbar_yi_ybar[i];
        }

        double rxy = sig_xi_xbar_yi_ybar / (Math.sqrt(sig_xi_xbar_sq * sig_yi_ybar_sq));
        return rxy;
    }


    public static void main(String[] args) {
        double[] x = {0, 1, 4, 6, 1, 8, 4, 3, 5};
        double[] y = {4, 5, 1, 2, 1, 5, 3, 4, 6};
        double[] z = {4, 2, 1, 3, 1, 1, 3, 2, 0};

        double rxy = getCorrelationXZ(x, y);
        double rxz = getCorrelationXZ(x, z);
        double ryz = getCorrelationXZ(y, z);

        double rxy_z = getPartialCorrelationControlledZ(rxy, rxz, ryz);
        System.out.println(rxy + " " + rxz + " " + ryz + " " + rxy_z);
    }


}