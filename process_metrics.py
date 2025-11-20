"""Metric processing pipeline for entropy-based scoring by region.

Usage:
    python process_metrics.py --input INPUT.xlsx --output OUTPUT.xlsx [--sheet Sheet1]

The input Excel file must contain a column named "区域" identifying the region for
entropy processing, plus the 10 metric columns:
- 19点前PI值
- 19点前复购率
- 订购渗透率
- 顾客退货率
- 门店退货率
- 销售额
- 价格弹性
- 少货率
- 门店毛利额
- 时段折扣额

The script applies boundary handling, transformations, entropy weighting per
region, and exports two sheets:
- sheet1: processed metrics with regional scores
- sheet2: per-region entropy weights and an aggregated weight sum
"""
from __future__ import annotations

import argparse
import math
from typing import Dict, Iterable, List, Tuple

import pandas as pd

# Metric names
METRICS: List[str] = [
    "19点前PI值",
    "19点前复购率",
    "订购渗透率",
    "顾客退货率",
    "门店退货率",
    "销售额",
    "价格弹性",
    "少货率",
    "门店毛利额",
    "时段折扣额",
]

NEGATIVE_METRICS = {"顾客退货率", "门店退货率", "时段折扣额", "价格弹性", "少货率"}

# Boundary rules: (lower, upper) where None means no cap
BOUNDARY_RULES: Dict[str, Tuple[float | None, float | None]] = {
    "19点前PI值": (0.0, 1.0),
    "19点前复购率": (0.0, 1.0),
    "订购渗透率": (0.0, 1.0),
    "顾客退货率": (-1.0, 0.0),
    "门店退货率": (-1.0, 0.0),
}


def apply_boundaries(df: pd.DataFrame) -> pd.DataFrame:
    """Apply metric-specific boundary capping.

    Metrics without explicit rules are left unchanged.
    """
    bounded = df.copy()
    for metric in METRICS:
        lower, upper = BOUNDARY_RULES.get(metric, (None, None))
        if lower is not None:
            bounded[metric] = bounded[metric].clip(lower=lower)
        if upper is not None:
            bounded[metric] = bounded[metric].clip(upper=upper)
    return bounded


def transform_metrics(df: pd.DataFrame) -> pd.DataFrame:
    """Apply fixed transformations after boundary handling."""
    transformed = df.copy()
    for metric in ["顾客退货率", "门店退货率", "少货率", "销售额"]:
        transformed[metric] = transformed[metric] ** 2

    transformed["门店毛利额"] = transformed["门店毛利额"].abs() ** 1.5
    transformed["时段折扣额"] = transformed["时段折扣额"].abs()
    return transformed


def normalize_region(df: pd.DataFrame, metrics: Iterable[str]) -> pd.DataFrame:
    """Normalize metrics to 0-1 within a region using forward or inverse formula."""
    normalized = pd.DataFrame(index=df.index)
    for metric in metrics:
        col = df[metric]
        max_val = col.max()
        min_val = col.min()
        if math.isclose(max_val, min_val):
            normalized[metric] = 0.0
            continue

        if metric in NEGATIVE_METRICS:
            normalized[metric] = (max_val - col) / (max_val - min_val)
        else:
            normalized[metric] = (col - min_val) / (max_val - min_val)
    return normalized


def entropy_weights(normalized: pd.DataFrame) -> pd.Series:
    """Compute entropy weights for normalized metrics."""
    if normalized.empty:
        return pd.Series({metric: 0.0 for metric in normalized.columns})

    sums = normalized.sum(axis=0)
    p = normalized.divide(sums.replace(0, pd.NA)).fillna(0.0)
    n = normalized.shape[0]
    k = 1.0 / math.log(n) if n > 1 else 0.0

    entropy_components = p * (p.where(p > 0).applymap(math.log))
    entropy = -k * entropy_components.sum(axis=0)

    diversity = 1 - entropy
    total_diversity = diversity.sum()
    if math.isclose(total_diversity, 0.0):
        weights = pd.Series(1.0 / len(diversity), index=diversity.index)
    else:
        weights = diversity / total_diversity
    return weights


def score_region(df_region: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
    """Compute scores and weights for a single region."""
    normalized = normalize_region(df_region, METRICS)
    weights = entropy_weights(normalized)
    raw_score = normalized.mul(weights, axis=1).sum(axis=1)

    mean = raw_score.mean()
    std = raw_score.std(ddof=0)
    if math.isclose(std, 0.0):
        z = pd.Series(0.0, index=raw_score.index)
    else:
        z = (raw_score - mean) / std

    normal_score = 0.5 * (1 + (z / math.sqrt(2)).apply(math.erf))
    index_score = normal_score * 100

    scores = pd.DataFrame({
        "区域内原始综合得分": raw_score,
        "区域内正态0-1得分": normal_score,
        "区域内指数0-100得分": index_score,
    })
    return scores, weights


def process(input_path: str, output_path: str, sheet_name: str | None = None) -> None:
    df = pd.read_excel(input_path, sheet_name=sheet_name)
    required_columns = {"区域", *METRICS}
    missing = required_columns - set(df.columns)
    if missing:
        raise ValueError(f"Missing required columns: {', '.join(sorted(missing))}")

    metric_data = df[METRICS]
    metric_data = apply_boundaries(metric_data)
    metric_data = transform_metrics(metric_data)

    processed = df.copy()
    processed[METRICS] = metric_data

    sheet1_frames = []
    weight_rows = []

    for region, region_df in processed.groupby("区域"):
        scores, weights = score_region(region_df)
        region_output = pd.concat([region_df, scores], axis=1)
        sheet1_frames.append(region_output)

        weight_row = weights.reindex(METRICS, fill_value=0.0)
        weight_row["销售额+门店毛利额+时段折扣额权重和"] = weight_row[
            ["销售额", "门店毛利额", "时段折扣额"]
        ].sum()
        weight_rows.append(pd.DataFrame([weight_row], index=[region]))

    sheet1 = pd.concat(sheet1_frames, axis=0)
    sheet2 = pd.concat(weight_rows, axis=0)
    sheet2.index.name = "区域"

    with pd.ExcelWriter(output_path) as writer:
        sheet1.to_excel(writer, sheet_name="sheet1", index=False)
        sheet2.to_excel(writer, sheet_name="sheet2")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Process metrics with entropy weighting by region.")
    parser.add_argument("--input", required=True, help="Path to input Excel file")
    parser.add_argument("--output", required=True, help="Path to output Excel file")
    parser.add_argument("--sheet", default=None, help="Optional sheet name to read from input")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    process(args.input, args.output, sheet_name=args.sheet)


if __name__ == "__main__":
    main()
