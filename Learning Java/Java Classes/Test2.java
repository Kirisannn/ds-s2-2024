
class Test2 {

    static void Swap(Integer j, Integer k) {
        int tmp = k.intValue();
        k = new Integer(j.intValue());
        j = new Integer(tmp);
    }

    public static void main(String[] args) {
        Integer n = new Integer(5), m = new Integer(6);
        Swap(n, m);
        System.out.println("n = " + n + "; m = " + m);
    }
}
