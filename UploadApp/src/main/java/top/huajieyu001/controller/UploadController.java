package top.huajieyu001.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.huajieyu001.domain.*;
import top.huajieyu001.service.FileUploadRecordService;

import javax.annotation.Resource;

/**
 * @Author huajieyu
 * @Date 2026/3/19 1:35
 * @Version 1.0
 * @Description TODO
 */
@RestController
@RequestMapping("/api/file")
public class UploadController {

    @Resource
    private FileUploadRecordService fileUploadRecordService;

    @PostMapping("/check")
    public Result<UploadCheckVO> check(@RequestBody UploadCheckRequest checkRequest) {
        return Result.success(fileUploadRecordService.check(checkRequest));
    }

    @PostMapping("/chunk")
    public Result<ChunkUploadVO> chunk(String fileMd5, String fileName, Integer chunkIndex, Integer totalChunks, MultipartFile chunkFile) {
        ChunkUploadRequest chunkUploadRequest = new ChunkUploadRequest();
        chunkUploadRequest.setFileMd5(fileMd5);
        chunkUploadRequest.setFileName(fileName);
        chunkUploadRequest.setChunkIndex(chunkIndex);
        chunkUploadRequest.setTotalChunks(totalChunks);
        chunkUploadRequest.setChunkFile(chunkFile);
        return Result.success(fileUploadRecordService.chunkUpload(chunkUploadRequest));
    }

    @PostMapping("/merge")
    public Result<MergeVO> merge(@RequestBody MergeRequest mergeRequest) {
        return Result.success(fileUploadRecordService.merge(mergeRequest));
    }

    @GetMapping("/progress/{fileMd5}")
    public Result<UploadProgressVO> progress(@PathVariable String fileMd5) {
        return Result.success(fileUploadRecordService.progress(fileMd5));
    }
}
