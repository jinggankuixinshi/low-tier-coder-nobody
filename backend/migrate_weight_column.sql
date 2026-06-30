-- ============================================================
-- 修复 evaluation_indicator.weight 列精度
-- decimal(5,4) 仅可容纳 -9.9999~9.9999，无法存储 0~100 权重值
-- 扩大为 decimal(5,2)，范围 -999.99~999.99
-- ============================================================
ALTER TABLE evaluation_indicator MODIFY COLUMN weight DECIMAL(5,2) DEFAULT 0.00 COMMENT '指标权重（0~100 整数百分比）';
