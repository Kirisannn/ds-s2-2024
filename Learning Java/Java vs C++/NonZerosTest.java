
public class NonZerosTest {

    public static int[] NonZeros(int[] A) {
        int arrLen = A.length;
        int[] result = new int[arrLen];

        // Printing first section "passing [x1,x2,x3,x4,x5] got back ["
        System.out.print("passing [");
        for (int i = 0; i < arrLen; i++) {
            System.out.print(A[i]);
            if (i != arrLen - 1) {
                System.out.print(",");
            }
        }
        System.out.print("] got back [");

        boolean frontZero = false, noPrint = true;
        for (int i = 0; i < arrLen; i++) {
            if (A[i] != 0) {
                if (i == 0) {
                    System.out.print(A[i]);
                } else if (frontZero == true) {
                    System.out.print(A[i]);
                } else {
                    System.out.print("," + A[i]);
                }
                frontZero = false;
                noPrint = false;
            } else if (A[i] == 0 && noPrint == true) {
                frontZero = true;
            }
        }
        System.out.println("]");

        return result;
    }

    public static void main(String[] args) {
        int[] a = {0, 1, 2, 3, 2};
        int[] b = {0, 0};
        int[] c = {22, 0, -5, 0, 126};
        int[] d = {1, 0};

        NonZeros(a);
        NonZeros(b);
        NonZeros(c);
        NonZeros(d);
    }

}
