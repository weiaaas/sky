package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.DocFlavor;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 数据统计相关接口
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "数据统计相关接口")
public class ReportController {
    @Autowired
    private ReportService reportService;

    /*
    统计营业额
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> TurnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        TurnoverReportVO turnoverReportVO = reportService.TurnoverStatistics(begin, end);
        log.info("营业额统计：{}，{}", begin, end);
        return Result.success(turnoverReportVO);
    }

    /**
     * 用户新增量与总量
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("用户统计：{}，{}", begin, end);
         return Result.success(reportService.userStatistics(begin,end));
    }

    /**
     * 每日总订单与有效订单
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单统计：{}，{}", begin, end);
        return Result.success(reportService.ordersStatistics(begin,end));
    }

    /**
     * 查询前10的商品
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("商品排名前10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("查询前10菜品：{}，{}", begin, end);
        return Result.success(reportService.top10(begin,end));
    }

    /**
     * 导出运营数据
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response){
          reportService.exportBusinessData(response);
    }
}
