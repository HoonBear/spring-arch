package com.restApi.restApiSpringBootApp.controller.v1;

import com.restApi.restApiSpringBootApp.advice.exception.CCommunicationException;
import com.restApi.restApiSpringBootApp.advice.exception.CSocialAgreementException;
import com.restApi.restApiSpringBootApp.advice.exception.CUserExistException;
import com.restApi.restApiSpringBootApp.advice.exception.CUserNotFoundException;
import com.restApi.restApiSpringBootApp.config.security.JwtProvider;
import com.restApi.restApiSpringBootApp.domain.user.User;
import com.restApi.restApiSpringBootApp.domain.user.UserJpaRepo;
import com.restApi.restApiSpringBootApp.dto.jwt.TokenDto;
import com.restApi.restApiSpringBootApp.dto.jwt.TokenRequestDto;
import com.restApi.restApiSpringBootApp.dto.sign.UserLoginRequestDto;
import com.restApi.restApiSpringBootApp.dto.sign.UserSignupRequestDto;
import com.restApi.restApiSpringBootApp.dto.sign.UserSocialLoginRequestDto;
import com.restApi.restApiSpringBootApp.dto.sign.UserSocialSignupRequestDto;
import com.restApi.restApiSpringBootApp.dto.social.KakaoProfile;
import com.restApi.restApiSpringBootApp.model.response.CommonResult;
import com.restApi.restApiSpringBootApp.model.response.SingleResult;
import com.restApi.restApiSpringBootApp.service.response.ResponseService;
import com.restApi.restApiSpringBootApp.service.security.SignService;
import com.restApi.restApiSpringBootApp.service.user.KakaoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@Api(tags = {"1. SignUp/LogIn"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
public class SignController {

    private final JwtProvider jwtProvider;
    private final UserJpaRepo userJpaRepo;
    private final KakaoService kakaoService;
    private final SignService signService;
    private final ResponseService responseService;

    @ApiOperation(value = "?????????", notes = "???????????? ???????????? ?????????.")
    @PostMapping("/login")
    public SingleResult<TokenDto> login(
            @ApiParam(value = "????????? ?????? DTO", required = true)
            @RequestBody UserLoginRequestDto userLoginRequestDto) {

        TokenDto tokenDto = signService.login(userLoginRequestDto);
        return responseService.getSingleResult(tokenDto);
    }

    @ApiOperation(value = "????????????", notes = "??????????????? ?????????.")
    @PostMapping("/signup")
    public SingleResult<Long> signup(
            @ApiParam(value = "?????? ?????? ?????? DTO", required = true)
            @RequestBody UserSignupRequestDto userSignupRequestDto) {
        Long signupId = signService.signup(userSignupRequestDto);
        return responseService.getSingleResult(signupId);
    }

    @ApiOperation(
            value = "?????????, ???????????? ?????? ?????????",
            notes = "????????? ?????? ????????? ?????? ?????? ??? ???????????? ????????? ???????????? ????????? ????????? ???????????? ????????? ??????????????????.")
    @PostMapping("/reissue")
    public SingleResult<TokenDto> reissue(
            @ApiParam(value = "?????? ????????? ?????? DTO", required = true)
            @RequestBody TokenRequestDto tokenRequestDto) {
        return responseService.getSingleResult(signService.reissue(tokenRequestDto));
    }

    @ApiOperation(
            value = "?????? ????????? - kakao",
            notes = "???????????? ???????????? ?????????.")
    @PostMapping("/social/login/kakao")
    public SingleResult<TokenDto> loginByKakao(
            @ApiParam(value = "?????? ????????? dto", required = true)
            @RequestBody UserSocialLoginRequestDto socialLoginRequestDto) {

        KakaoProfile kakaoProfile = kakaoService.getKakaoProfile(socialLoginRequestDto.getAccessToken());
        if (kakaoProfile == null) throw new CUserNotFoundException();

        User user = userJpaRepo.findByEmailAndProvider(kakaoProfile.getKakao_account().getEmail(), "kakao")
                .orElseThrow(CUserNotFoundException::new);
        return responseService.getSingleResult(jwtProvider.createTokenDto(user.getUserId(), user.getRoles()));
    }

    @ApiOperation(
            value = "?????? ???????????? - kakao",
            notes = "???????????? ??????????????? ?????????."
    )
    @PostMapping("/social/signup/kakao")
    public CommonResult signupBySocial(
            @ApiParam(value = "?????? ???????????? dto", required = true)
            @RequestBody UserSocialSignupRequestDto socialSignupRequestDto) {

        KakaoProfile kakaoProfile =
                kakaoService.getKakaoProfile(socialSignupRequestDto.getAccessToken());
        if (kakaoProfile == null) throw new CUserNotFoundException();
        if (kakaoProfile.getKakao_account().getEmail() == null) {
            kakaoService.kakaoUnlink(socialSignupRequestDto.getAccessToken());
            throw new CSocialAgreementException();
        }

        Long userId = signService.socialSignup(UserSignupRequestDto.builder()
                .email(kakaoProfile.getKakao_account().getEmail())
                .name(kakaoProfile.getProperties().getNickname())
                .nickName(kakaoProfile.getProperties().getNickname())
                .provider("kakao")
                .build());

        return responseService.getSingleResult(userId);
    }
}
