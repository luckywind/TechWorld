package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-23
 * @Desc
 */
public class NumMatrix {

  private int[][] preSum;
  public NumMatrix(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    if (m == 0 || n == 0) return;
    // 构造前缀和矩阵
    preSum = new int[m + 1][n + 1]; //注意，任意一个纬度下标是0时，该矩阵是不存在的，所以和是0。所以preSum两个下标都从1开始，对应matrix的行数，而不是行索引
    for (int i = 1; i <= m; i++) {   //
      for (int j = 1; j <= n; j++) {
        // 计算每个矩阵 [0, 0, i, j] 的元素和
        //利用已有结果计算
        preSum[i][j] =
            preSum[i-1][j]
            + preSum[i][j-1]
            + matrix[i - 1][j - 1]
            - preSum[i-1][j-1];
      }
    }
  }

  // 计算子矩阵 [x1, y1, x2, y2] 的元素和
  public int sumRegion(int x1, int y1, int x2, int y2) {
    // 目标矩阵之和由四个相邻矩阵运算获得
    return preSum[x2+1][y2+1] - preSum[x1][y2+1] - preSum[x2+1][y1] + preSum[x1][y1];
  }


  public static void main(String[] args) {
    NumMatrix numMatrix = new NumMatrix(new int[][]{new int[]{3,0,1,4,2},
        new int[]{5,6,3,2,1},
    new int[]{1,2,0,1,5},
    new int[]{4,1,0,1,7},
    new int[]{1,0,3,0,5}});
    System.out.println(numMatrix.sumRegion(0,0,0,0));//return 8 (红色矩形框的元素总和)
//    System.out.println(numMatrix.sumRegion(2, 1, 4, 3));//return 8 (红色矩形框的元素总和)
//    numMatrix.sumRegion(1, 1, 2, 2); // return 11 (绿色矩形框的元素总和)
//    numMatrix.sumRegion(1, 2, 2, 4); // return 12 (蓝色矩形框的元素总和)


  }
}
