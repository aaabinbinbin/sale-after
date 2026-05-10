package com.aftersales.common.result;

import java.util.Collections;
import java.util.List;

/**
 * 分页统一返回体。
 *
 * 封装 MyBatis 分页查询结果，方便前端和 Agent 统一消费。
 */
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页大小 */
    private int pageSize;

    /** 总页数 */
    private int pages;

    private PageResult() {}

    /**
     * 构建分页结果。
     *
     * @param records  当前页数据
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> pr = new PageResult<>();
        pr.records = records != null ? records : Collections.emptyList();
        pr.total = total;
        pr.pageNum = pageNum;
        pr.pageSize = pageSize;
        pr.pages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        return pr;
    }

    /** 空页 */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(Collections.emptyList(), 0, pageNum, pageSize);
    }

    // ========== getter / setter ==========

    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }
}
