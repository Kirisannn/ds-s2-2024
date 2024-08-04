
class ArrayCopyExample {

    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, b = new int[a.length];
        System.arraycopy(a, 0, b, 0, 3);
        System.out.print("a: [");
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                System.out.print("," + a[i]);
            } else {
                System.out.print(a[i]);
            }
        }
        System.out.print("]\t");

        System.out.print("b: [");
        for (int i = 0; i < b.length; i++) {
            if (i != 0) {
                System.out.print("," + b[i]);
            } else {
                System.out.print(b[i]);
            }
        }
        System.out.println("]");

        System.arraycopy(a, 7, b, 7, 3);

        System.out.print("b: [");
        for (int i = 0; i < b.length; i++) {
            if (i != 0) {
                System.out.print("," + b[i]);
            } else {
                System.out.print(b[i]);
            }
        }
        System.out.println("]");
    }
}
