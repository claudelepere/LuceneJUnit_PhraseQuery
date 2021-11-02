package com.makble.lucenetest;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 * http://makble.com/what-is-term-vector-in-lucene
 * @author claud
 *
 */
public class TestTermVector {
    private static Analyzer          analyzer     = new StandardAnalyzer();
    private static IndexWriterConfig config       = new IndexWriterConfig(analyzer);
    private static RAMDirectory      ramDirectory = new RAMDirectory();
    private static IndexWriter       indexWriter;

    public static void main(String[] args) {
        FieldType t = new FieldType();
        t.setStored(true);
//      t.setIndexOptions(IndexOptions.DOCS);
//      t.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
//      t.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
        t.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        t.setStoreTermVectors(true);
        t.setStoreTermVectorOffsets(true);
        t.setStoreTermVectorPayloads(true);
        t.setStoreTermVectorPositions(true);
        t.freeze();
        
        Document doc = new Document();
//      doc.add(new Field("title", "quick fox brown fox", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
//      doc.add(new Field("body", "quick fox run faster", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field("title", "quick fox brown fox zizi zizi zizi", t));
        doc.add(new Field("body", "quick fox run faster", t));
        doc.add(new Field("name", "zizi ist zizi", t));
        
        try {
            indexWriter = new IndexWriter(ramDirectory, config);
            indexWriter.addDocument(doc);
            indexWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            IndexReader   idxReader   = DirectoryReader.open(ramDirectory);
            IndexSearcher idxSearcher = new IndexSearcher(idxReader);
            // The returned Fields instance acts like a single-document 
            // inverted index (the docID will be 0).
            Terms  terms  = idxReader.getTermVector(0, "title");
            Fields fields = idxReader.getTermVectors(0);
            System.out.println("|" + fields.size());
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                BytesRef bytesRef;
                PostingsEnum postingsEnum = null;
                while((bytesRef=termsEnum.next()) != null) {
                    System.out.println("BytesRef: " + bytesRef.utf8ToString());
                    System.out.println("docFreq: " + termsEnum.docFreq());
                    System.out.println("totalTermFreq: " + termsEnum.totalTermFreq());
                    Bits liveDocs = null;
//                  postingsEnum =termsEnum.postings(liveDocs, postingsEnum, PostingsEnum.ALL);
                    postingsEnum = termsEnum.postings(liveDocs, postingsEnum, PostingsEnum.NONE);
                    while(postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        System.out.println("docID: " + postingsEnum.docID());
                        int freq = postingsEnum.freq();
                        System.out.println("freq: " + freq);
                        while (freq-- > 0) {
                            int position = postingsEnum.nextPosition();
                            System.out.println("position: " + position);
                            System.out.println("startOffset: " + postingsEnum.startOffset());
                            System.out.println("endOffset: " + postingsEnum.endOffset());
//                          System.out.println("payload: " + postingsEnum.getPayload());
                            
                        }
                        
                    }
                    System.out.println();
                }
            }
            
            PostingsHighlighter highlighter = new PostingsHighlighter();
            Query query = new TermQuery(new Term("title", "zizi"));
            TopDocs topDocs = idxSearcher.search(query, 10);
            String[] highlights = highlighter.highlight("title", query, idxSearcher, topDocs);
            Arrays.asList(highlights).stream().forEach(System.out::print);
        
            ScoreDoc[] scoreDocZ = topDocs.scoreDocs;
            System.out.println(scoreDocZ.length); 
            for (ScoreDoc score : scoreDocZ){
                System.out.println("DOC " + score.doc + " SCORE " + score.score);
                int docID = score.doc;
                Document document = idxSearcher.doc(docID);
                System.out.println(document.getFields());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

