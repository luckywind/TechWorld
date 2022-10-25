package leedCode;

public class ColorOrder {
    public static void main(String[] args) {
        int[] arr = {0, 1, 2, 1, 2, 2, 1, 0, 0};

//        int zero = 0, two = arr.length - 1;
//        for (int i = 0; i < arr.length; i++) {
//            if (arr[i] == 2) {
//                swap(arr, i, two);
//                two--;
//            } else if (arr[i] == 0) {
//                swap(arr, zero, i);
//                zero++;
//            }
//        }

//        sortColors(arr);
        AnothersortColors(arr);
        for (int i = 0; i < arr.length; i++) {
            System.out.println(arr[i]);
        }

    }




    public static void swap(int[] arr,int i,int j) {
        int tmp;
        tmp = arr[j];
        arr[j] = arr[i];
        arr[i] = tmp;
    }

    public static void sortColors(int[] A) {
        if (A == null) {
            return;
        }
        int count = 0; // 统计1的个数
        int sum = 0; // 统计数组的和
        for (int i : A) {
            if (i == 1) {
                count++;
            }
            sum += i;
        }
        sum = (sum - count) /2; // 计算2的数目
        count = A.length - count - sum; // 1開始出现的位置
        sum = A.length - sum; // 2開始出现的位置
        for (int i = 0; i < count; i++) {
            A[i] = 0;
        }
        for (int i = count; i < sum; i++) {
            A[i] = 1;
        }
        for (int i = sum; i < A.length; i++) {
            A[i] = 2;
        }
    }


    public static  void AnothersortColors(int[] A) {
        int left =0;
        int right = A.length-1;
        int i=0;
        while(i<=right){
            if(A[i]==0){
                swap(A,i++,left++);
            }else if(A[i]==2)
                swap(A,i,right--);
            else
                i++;
        }
    }


}
