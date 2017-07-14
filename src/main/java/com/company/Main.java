package com.company;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Sum;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

/**
 * メイン
 * Created by sekiguchikai on 2017/07/14.
 */
public class Main {
    /**
     * 関数オブジェクト
     * 与えられたString str, String numを","で分割し、
     * numをInteger型に変更して、KV<String, Integer>型にする
     */
    static class SplitWordsAndMakeKVFn extends DoFn<String, KV<String, Integer>> {
        @ProcessElement
        // ProcessContextは、inputを表すobject
        // 自分で定義しなくてもBeam SDKが勝手に取ってきてくれる
        public void processElement(ProcessContext c) {
            // ","で分割
            String[] words = c.element().split(",");
            // 分割したword[0]をKに、words[1]をIntegerに変換してVにする
            c.output(KV.of(words[0], Integer.parseInt(words[1])));
        }
    }

    /**
     * 関数オブジェクト
     * KV<String, Iterable<Integer>型をString型に変更する
     */
    static class TransTypeFromKVIterableAndMakeStringFn extends DoFn<KV<String, Iterable<Integer>>, String> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            // inputをString型に変換する
            c.output(String.valueOf(c.element()));

        }
    }

    /**
     * 関数オブジェクト
     * KV<String, Iterable<Integer>型をString型に変更する
     */
    static class TransTypeFromKVAndMakeStringFn extends DoFn<KV<String, Integer>, String> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            // inputをString型に変換する
            c.output(String.valueOf(c.element()));

        }

    }


    /**
     * インプットデータのパス
     */
    private static final String INPUT_FILE_PATH = "./sample.txt";
    /**
     * アウトデータのパス
     */
    private static final String GROUPBYKEY_OUTPUT_FILE_PATH = "./src/main/resources/groupbykey_result/result.csv";

    private static final String COMBINE_OUTPUT_FILE_PATH = "./src/main/resources/combine_result/result.csv";

    /**
     * メイン
     * 理解のため、メソッドチェーンを極力使用していない
     * そのため、冗長なコードになっている
     *
     * @param args 引数
     */
    public static void main(String[] args) {
        // まずPipelineに設定するOptionを作成する
        // 今回は、ローカルで起動するため、DirectRunnerを指定する
        // ローカルモードでは、DirectRunnerがすでにデフォルトになっているため、ランナーを設定する必要はない
        PipelineOptions options = PipelineOptionsFactory.create();

        // Optionを元にPipelineを生成する
        Pipeline pipeline = Pipeline.create(options);

        // inout dataを読み込んで、そこからPCollection(パイプライン内の一連のデータ)を作成する
        PCollection<String> lines = pipeline.apply(TextIO.read().from(INPUT_FILE_PATH));

        //　与えられたString str, String numを","で分割し、numをInteger型に変更して、KV<String, Integer>型にする
        PCollection<KV<String, Integer>> kvCounter = lines.apply(ParDo.of(new SplitWordsAndMakeKVFn()));

        // GroupByKeyで、{Go, [2, 9, 1, 5]}のような形にする
        // GroupByKey.<K, V>create())でGroupByKey<K, V>を生成している
        PCollection<KV<String, Iterable<Integer>>> groupedWords = kvCounter.apply(
                GroupByKey.<String, Integer>create());

        PCollection<KV<String, Integer>> sumPerKey = kvCounter
                .apply(Sum.<String>integersPerKey());


        // 出力のため、<KV<String, Iterable<Integer>>>型からString型に変換している
        PCollection<String> groupedOutput = groupedWords.apply(ParDo.of(new TransTypeFromKVIterableAndMakeStringFn()));
        groupedOutput.apply(TextIO.write().to(GROUPBYKEY_OUTPUT_FILE_PATH));

        PCollection<String> output = sumPerKey.apply(ParDo.of(new TransTypeFromKVAndMakeStringFn()));
        // 書き込む
        output.apply(TextIO.write().to(COMBINE_OUTPUT_FILE_PATH));

        // run : PipeLine optionで指定したRunnerで実行
        // waitUntilFinish : PipeLineが終了するまで待って、最終的な状態を返す
        pipeline.run().waitUntilFinish();
    }


}
