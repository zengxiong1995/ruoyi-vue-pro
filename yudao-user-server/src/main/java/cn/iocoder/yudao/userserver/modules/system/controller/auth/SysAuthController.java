package cn.iocoder.yudao.userserver.modules.system.controller.auth;

import cn.iocoder.yudao.coreservice.modules.system.service.social.SysSocialService;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.userserver.modules.system.controller.auth.vo.*;
import cn.iocoder.yudao.userserver.modules.system.service.auth.SysAuthService;
import cn.iocoder.yudao.userserver.modules.system.service.sms.SysSmsCodeService;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getUserAgent;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Api(tags = "认证")
@RestController
@RequestMapping("/")
@Validated
@Slf4j
public class SysAuthController {

    @Resource
    private SysAuthService authService;
    @Resource
    private SysSmsCodeService smsCodeService;
    @Resource
    private SysSocialService socialService;


    @PostMapping("/login")
    @ApiOperation("使用手机 + 密码登录")
    public CommonResult<SysAuthLoginRespVO> login(@RequestBody @Valid SysAuthLoginReqVO reqVO) {
        String token = authService.login(reqVO, getClientIP(), getUserAgent());
        // 返回结果
        return success(SysAuthLoginRespVO.builder().token(token).build());
    }

    @PostMapping("/sms-login")
    @ApiOperation("使用手机 + 验证码登录")
    public CommonResult<SysAuthLoginRespVO> smsLogin(@RequestBody @Valid SysAuthSmsLoginReqVO reqVO) {
        String token = authService.smsLogin(reqVO, getClientIP(), getUserAgent());
        // 返回结果
        return success(SysAuthLoginRespVO.builder().token(token).build());
    }

    @PostMapping("/send-sms-code")
    @ApiOperation("发送手机验证码")
    public CommonResult<Boolean> sendSmsCode(@RequestBody @Valid SysAuthSendSmsReqVO reqVO) {
        smsCodeService.sendSmsCode(reqVO.getMobile(), reqVO.getScene(), getClientIP());
        return success(true);
    }

    @PostMapping("/reset-password")
    @ApiOperation(value = "重置密码", notes = "用户忘记密码时使用")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid MbrAuthResetPasswordReqVO reqVO) {
        return null;
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @ApiOperation("社交授权的跳转")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "社交类型", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "redirectUri", value = "回调路径", dataTypeClass = String.class)
    })
    public CommonResult<String> socialAuthRedirect(@RequestParam("type") Integer type,
                                                   @RequestParam("redirectUri") String redirectUri) {
        return CommonResult.success(socialService.getAuthorizeUrl(type, redirectUri));
    }

    // TODO @timfruit：这个接口，是要删除的么？
    @GetMapping("/social-login-get")
    @ApiOperation("微信公众号授权回调地址，输出social-login2的必要参数用于测试，使用 code 授权码")
    @ResponseBody
    @Deprecated
    public CommonResult<MbrAuthSocialLoginReqVO> socialLoginGet(HttpServletRequest request,String code,String state) {
        // 返回结果
        MbrAuthSocialLoginReqVO reqVO = MbrAuthSocialLoginReqVO.builder().state(state).code(code).build();
        reqVO.setType(12);
        //输出social-login2的必要参数用于测试
        System.out.println(JSON.toJSON(reqVO));
        return success(reqVO);
    }

    @PostMapping("/social-login")
    @ApiOperation("社交登录，使用 code 授权码")
    public CommonResult<SysAuthLoginRespVO> socialLogin(@RequestBody @Valid MbrAuthSocialLoginReqVO reqVO) {
        String token = authService.socialLogin(reqVO, getClientIP(), getUserAgent());
        return success(SysAuthLoginRespVO.builder().token(token).build());
    }

    // TODO @timfruit：社交登陆时，使用手机验证码来验证哈。这块我当时没设计好，改改，嘿嘿。

    @PostMapping("/social-login2")
    @ApiOperation("社交登录，使用 code 授权码 + 账号密码")
    public CommonResult<SysAuthLoginRespVO> socialLogin2(@RequestBody @Valid MbrAuthSocialLogin2ReqVO reqVO) {
        String token = authService.socialLogin2(reqVO, getClientIP(), getUserAgent());
        return success(SysAuthLoginRespVO.builder().token(token).build());
    }

    @PostMapping("/social-bind")
    @ApiOperation("社交绑定，使用 code 授权码")
    public CommonResult<Boolean> socialBind(@RequestBody @Valid MbrAuthSocialBindReqVO reqVO) {
        authService.socialBind(getLoginUserId(), reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/social-unbind")
    @ApiOperation("取消社交绑定")
    public CommonResult<Boolean> socialUnbind(@RequestBody MbrAuthSocialUnbindReqVO reqVO) {
        socialService.unbindSocialUser(getLoginUserId(), reqVO.getType(), reqVO.getUnionId(), UserTypeEnum.MEMBER);
        return CommonResult.success(true);
    }

}
