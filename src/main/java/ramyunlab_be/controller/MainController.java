package ramyunlab_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ValidationException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ramyunlab_be.dto.FavoriteDTO;
import ramyunlab_be.dto.RamyunDTO;
import ramyunlab_be.dto.RamyunInfo;
import ramyunlab_be.dto.ResDTO;
import ramyunlab_be.dto.RamyunFilterDTO;
import ramyunlab_be.dto.ReviewDTO;
import ramyunlab_be.service.FavoriteService;
import ramyunlab_be.service.MainService;
import ramyunlab_be.service.ReviewService;
import ramyunlab_be.vo.StatusCode;

@Slf4j
@Tag(name = "Main", description = "메인 페이지 라면 데이터 관련 API 명세")
@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {

  private final MainService mainService;
  private final ReviewService reviewService;
  private final FavoriteService favoriteService;

  @Operation(summary = "모든 라면 정보 조회", description = "메인페이지 전체 라면 정보를 조회함 (+페이지네이션) ")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", useReturnTypeSchema = true, description = "데이터 조회 성공"),
      @ApiResponse(responseCode = "400", useReturnTypeSchema = true, description = "데이터 조회 실패")
  })
  @GetMapping
  public ResponseEntity<ResDTO<Object>> getAllList (@Parameter(name="page", description = "현재 페이지 번호", in = ParameterIn.QUERY, example = "1")
                                                      @RequestParam(name="page", required = false) Integer page,
                                                    @Parameter(name="sort", description = "정렬 기준", in = ParameterIn.QUERY, example = "name")
                                                      @RequestParam(value="sort", required = false) String sort,
                                                    @Parameter(name="direction", description = "정렬 순서", in = ParameterIn.QUERY, example = "desc")
                                                      @RequestParam(value = "direction", required = false) String direction,
                                                    @Parameter(name="searchDTO", description = "필터링 조건", in = ParameterIn.QUERY)
                                                      RamyunFilterDTO filter,
                                                      @AuthenticationPrincipal String userIdx){
    Long user = null;
    if(userIdx != null && !userIdx.equals("anonymousUser")) {
      user = Long.parseLong(userIdx);
    }

    if(userIdx != null && !userIdx.equals("anonymousUser")) {
      userIdx = null;
    }

    // 라면 목록 조회
    if(page == null) page = 1;
    Page<RamyunDTO> result = mainService.getRamyunList(page, sort, direction, filter, user);

    return ResponseEntity.ok().body(ResDTO.builder()
                                          .statusCode(StatusCode.OK)
                                          .message("데이터 조회 성공")
                                          .data(result)
                                          .build());
  }

    @Operation(summary = "필터링된 라면 정보 조회",
               description = "조건을 걸어 필터링한 라면 정보를 조회함 (+페이지네이션)\n"
                             + "리뷰 페이지네이션 시 page 추가")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", useReturnTypeSchema = true, description = "데이터 조회 성공"),
        @ApiResponse(responseCode = "400", useReturnTypeSchema = true, description = "데이터 조회 실패")
    })
    @GetMapping("/search")
    public ResponseEntity<ResDTO<Object>> getFilteredList(@Parameter(name="page", description = "현재 페이지 번호", in = ParameterIn.QUERY, example = "1")
                                                            @RequestParam(name="page", required = false) Integer page,
                                                          @Parameter(name="sort", description = "정렬 기준", in = ParameterIn.QUERY, example = "name")
                                                            @RequestParam(value="sort", required = false) String sort,
                                                          @Parameter(name="direction", description = "정렬 순서", in = ParameterIn.QUERY, example = "desc")
                                                            @RequestParam(value = "direction", required = false) String direction,
                                                          @Parameter(name="searchDTO", description = "필터링 조건", in = ParameterIn.QUERY)
                                                            RamyunFilterDTO filter,
                                                          @AuthenticationPrincipal String userIdx){

      Long user = null;
      if(userIdx != null && !userIdx.equals("anonymousUser")) {
        user = Long.parseLong(userIdx);
      }

      if(page == null) page = 1;

      Page<RamyunDTO> result = mainService.getRamyunList(page, sort, direction, filter, user);
      return ResponseEntity.ok().body(ResDTO.builder().statusCode(StatusCode.OK).message("데이터 조회 성공").data(result).build());
    }

  @Operation(summary = "라면 상세페이지 정보 조회",
             description = "라면 정보 및 라면에 대한 리뷰 첫 페이지를 조회함")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", useReturnTypeSchema = true, description = "데이터 조회 성공")
  })
  @GetMapping("/ramyun/{ramyunIdx}")
  public ResponseEntity<ResDTO<Object>> getRamyunInfo (@Parameter(name="ramyunIdx", description = "라면 인덱스", example = "1")
                                                         @PathVariable Long ramyunIdx,
                                                       @AuthenticationPrincipal String userIdx){

   // 로그인 유저 판별 후 찜 추가 여부 확인
    boolean isLiked = false;
    if(userIdx != null && !userIdx.equals("anonymousUser")) {
      isLiked = favoriteService.isLiked(Long.parseLong(userIdx), ramyunIdx);
    }

    // 라면 + 평점 조회
    RamyunDTO ramyun = mainService.getRamyun(ramyunIdx);

    // 리뷰 조회
    Page<ReviewDTO> reviews = reviewService.getReviewByRamyun(ramyunIdx, 1);

    // 베스트 리뷰 조회
    List<ReviewDTO> bestReview = reviewService.getBestReviewByRamyun(ramyunIdx);

    return ResponseEntity.ok().body(ResDTO.builder()
                                          .statusCode(StatusCode.OK)
                                          .message("데이터 조회 성공")
                                          .data(RamyunInfo.builder().ramyun(ramyun)
                                                          .bestReview(bestReview).review(reviews)
                                                          .isLiked(isLiked).build())
                                          .build());
  }

  @Operation(summary = "라면 리뷰 조회",
             description = "특정 라면에 대한 리뷰 정보를 조회함 (페이지네이션)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", useReturnTypeSchema = true, description = "데이터 조회 성공")
  })
  @GetMapping("/ramyun/{ramyunIdx}/review")
  public ResponseEntity<ResDTO<Object>> getRamyunReview (@Parameter(name="ramyunIdx", description = "라면 인덱스", example = "1")
                                                         @PathVariable Long ramyunIdx,
                                                         @Parameter(name="page", description = "페이지", example = "1")
                                                         @RequestParam(value = "page", required = false) Integer page,
                                                         @AuthenticationPrincipal String userIdx){
    // 리뷰 조회
    if(page == null) page = 1;
    Page<ReviewDTO> result = reviewService.getReviewByRamyun(ramyunIdx, page);

    return ResponseEntity.ok().body(ResDTO.builder().statusCode(StatusCode.OK)
                                          .message("리뷰 조회 성공")
                                          .data(RamyunInfo.builder().review(result).build()).build());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ResDTO> handleValidationException (ValidationException e) {
    return ResponseEntity
        .badRequest()
        .body(ResDTO.builder()
                    .statusCode(StatusCode.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
  }
}
