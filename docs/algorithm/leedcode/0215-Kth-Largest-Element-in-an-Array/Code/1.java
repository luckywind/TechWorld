class Solution {
    public int findKthLargest(int[] nums, int k) {
        // // ����һ��С���ѣ����ȶ���ģ�⣩
        PriorityQueue<Integer> heap =
            new PriorityQueue<Integer>();

        // �ڶ���ά����ǰ���k��Ԫ��
        for (int i = 0; i < nums.length; i++){
            if(heap.size() < k){
                heap.add(nums[i]);
            }else if (heap.element() < nums[i]){
                heap.poll();
                heap.add(nums[i]);
            }
        }
        return heap.poll();        
  }
}