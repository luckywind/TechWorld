public class Solution {
  public double findMedianSortedArrays(int[] nums1, int[] nums2) {
    // ʹnums1��Ϊ�϶�����,����������߼����ٶ�,ͬʱ���Ա���һЩ�߽�����
    if (nums1.length > nums2.length) {
      int[] temp = nums1;
      nums1 = nums2;
      nums2 = temp;
    }

    int len1 = nums1.length;
    int len2 = nums2.length;
    int leftLen = (len1 + len2 + 1) / 2; //������ϲ�&�����,���ߵĳ���
    
    // ������1���ж��ּ���
    int start = 0;
    int end = len1;
    while (start <= end) {
      // ��������ı�����A,B��λ��(��1��ʼ����)
      // count1 = 2 ��ʾ num1 ����ĵ�2������
      // ��index��1
      int count1 = start + ((end - start) / 2);
      int count2 = leftLen - count1;
      
      if (count1 > 0 && nums1[count1 - 1] > nums2[count2]) {
        // A��B��next��Ҫ��
        end = count1 - 1;
      } else if (count1 < len1 && nums2[count2 - 1] > nums1[count1]) {
        // B��A��next��Ҫ��
        start = count1 + 1;
      } else {
        // ��ȡ��λ��
        int result =  (count1 == 0)? nums2[count2 - 1]: // ��num1������������������ұ�
                      (count2 == 0)? nums1[count1 - 1]: // ��num2������������������ұ�
                      Math.max(nums1[count1 - 1], nums2[count2 - 1]); // �Ƚ�A,B
        if (isOdd(len1 + len2)) {
          return result;
        }

        // ����ż�����������
        int nextValue = (count1 == len1) ? nums2[count2]:
                        (count2 == len2) ? nums1[count1]:
                        Math.min(nums1[count1], nums2[count2]);
        return (result + nextValue) / 2.0;
      }
    }

    return Integer.MIN_VALUE; // ���Ե���������
  }

  // ��������true,ż������false
  private boolean isOdd(int x) {
    return (x & 1) == 1;
  }
}
