#  Copyright 2021 Collate
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import json
import os
from itertools import islice
from typing import Any

import pandas as pd
from pandas import DataFrame
from pyarrow import fs
from pyarrow.parquet import ParquetFile


def read_csv_from_s3(
    client: Any, key: str, bucket_name: str, sep: str = ",", sample_size: int = 100
) -> DataFrame:
    stream = client.get_object(Bucket=bucket_name, Key=key)["Body"]
    return pd.read_csv(stream, sep=sep, nrows=sample_size + 1)


def read_tsv_from_s3(
    client, key: str, bucket_name: str, sample_size: int = 100
) -> DataFrame:
    read_csv_from_s3(client, key, bucket_name, sep="\t", sample_size=sample_size)


def read_json_from_s3(
    client: Any, key: str, bucket_name: str, sample_size=100
) -> DataFrame:
    line_stream = client.get_object(Bucket=bucket_name, Key=key)["Body"].iter_lines()
    return pd.DataFrame.from_records(map(json.loads, line_stream), nrows=sample_size)


def read_parquet_from_s3(client: Any, key: str, bucket_name: str) -> DataFrame:
    s3 = fs.S3FileSystem(region=client.meta.region_name)
    return (
        ParquetFile(s3.open_input_file(os.path.join(bucket_name, key)))
        .schema.to_arrow_schema()
        .empty_table()
        .to_pandas()
    )
