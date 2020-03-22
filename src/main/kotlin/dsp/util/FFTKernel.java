package dsp.util;

import com.aparapi.Kernel;


public class FFTKernel extends Kernel {
    private final float[] wRe, wIm;
    private final float[][] re, im;
    private final int stepNum;
    private final int size;
    private final int[][] index;
    private final int[] bitRev;
    private final float[][] cr;
    private final float[][] ci;

    public FFTKernel(int n) {
        if (Integer.highestOneBit(n) != n) {
            throw new RuntimeException("N is not a power of 2: " + n);
        }
        this.re = new float[2][n];
        this.im = new float[2][n];
        this.wIm = new float[n / 2];
        this.wRe = new float[n / 2];
        this.size = n;
        stepNum = Integer.numberOfTrailingZeros(size);
        index = new int[stepNum][size];
        bitRev = new int[size];
        cr = new float[stepNum][size];
        ci = new float[stepNum][size];
        createBR();
        createW();
        createB();
        System.out.println("size = " + size);
        System.out.println("stepNum = " + stepNum);
/*
        for (int i = 0; i < stepNum; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(index[i][j] + " ");
            }
            System.out.println("");
        }
        for (int i = 0; i < stepNum; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(cr[i][j] + " ");
            }
            System.out.println("");
            for (int j = 0; j < size; j++) {
                System.out.print(ci[i][j] + " ");
            }
            System.out.println("\n");
        }
    */
    }

    public ComplexArray apply(float[] r) {
        if (r.length != size) throw new IllegalArgumentException("input length?");
        ComplexArray out = new ComplexArray();
        for (int i = 0; i < size; i++) {
            im[0][i] = 0f;
            re[0][i] = r[bitRev[i]];
        }
        execute(size, stepNum);
        out.real = re[(stepNum) % 2].clone();
        out.imag = im[(stepNum) % 2].clone();
        return out;
    }

    @Override
    public void run() {
        int i = getGlobalId();
        int m = getPassId();
        int j1 = m % 2;
        //int j2 = j1 == 1 ? 0 : 1;
        int j2 = (~j1) & 1;
        int p2m = 1 << m;
        if (i % (p2m * 2) < p2m) {
            re[j2][i] = re[j1][i] + cr[m][i] * re[j1][index[m][i]] - ci[m][i] * im[j1][index[m][i]];
            im[j2][i] = im[j1][i] + ci[m][i] * re[j1][index[m][i]] + cr[m][i] * im[j1][index[m][i]];
        } else {
            re[j2][i] = re[j1][index[m][i]] + cr[m][i] * re[j1][i] - ci[m][i] * im[j1][i];
            im[j2][i] = im[j1][index[m][i]] + ci[m][i] * re[j1][i] + cr[m][i] * im[j1][i];
        }
    }

    private void createBR() {
        bitRev[0] = 0;
        int a = 1;
        int b = size / 2;
        while (a < size) {
            for (int i = 0; i < a; i++) {
                bitRev[i + a] = bitRev[i] + b;
            }
            a *= 2;
            b /= 2;
        }
    }

    private void createB() {
        for (int i = 0; i < stepNum; i++) {
            int p2m = 1 << i;
            //int x = (stepNum+1)/p2m;
            int dk = (size) / (1 << (i + 1));

            for (int j = 0; j < size; j++) {
                if (j % (p2m * 2) < p2m) {
                    index[i][j] = j + p2m;
                    cr[i][j] = wRe[dk * (j % (p2m * 2))];
                    ci[i][j] = wIm[dk * (j % (p2m * 2))];
                } else {
                    index[i][j] = j - p2m;
                    //cr[i][j] = -wRe[x*(j % (p2m*2))];
                    //ci[i][j] = -wIm[x*(j % (p2m*2))];
                    cr[i][j] = -cr[i][j - p2m];
                    ci[i][j] = -ci[i][j - p2m];
                }
            }
        }
    }


    private void createW() {
        for (int i = 0; i < size / 2; i++) {
            double angle = -2 * i * Math.PI / size;
            wRe[i] = (float) Math.cos(angle);
            wIm[i] = (float) Math.sin(angle);
        }
    }
}
