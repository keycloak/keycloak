package org.keycloak.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collection;

@JsonDeserialize(builder = PagedResults.Builder.class)
public class PagedResults<T> {

    private int pageNum;
    private int pageSize;
    private Long totalHits;
    private Collection<T> results;

    private PagedResults(){}

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Long getTotalHits() {
        return totalHits;
    }

    public Collection<T> getResults() {
        return results;
    }

    public static class Builder<T> {

        private int pageNum;
        private int pageSize;
        private Long totalHits;
        private Collection<T> results;


        public Builder withResults(Collection<T> results){
            this.results = results;
            return this;
        }

        public Builder withTotalHits(long totalHits){
            this.totalHits = totalHits;
            return this;
        }

        public Builder withPageNum(int pageNum){
            this.pageNum = pageNum;
            return this;
        }

        public Builder withPageSize(int pageSize){
            this.pageSize = pageSize;
            return this;
        }

        public PagedResults<T> build(){
            PagedResults<T> resultsPage = new PagedResults<T>();
            resultsPage.results = this.results;
            resultsPage.totalHits = this.totalHits;
            resultsPage.pageNum = this.pageNum;
            resultsPage.pageSize = this.pageSize;
            return resultsPage;
        }
    }

}
