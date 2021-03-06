package info.mikaelsvensson.babyname.service;

import info.mikaelsvensson.babyname.service.util.auth.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static info.mikaelsvensson.babyname.service.util.auth.JwtFilter.ROLE_ADMIN;
import static info.mikaelsvensson.babyname.service.util.auth.JwtFilter.ROLE_USER;

@EnableWebSecurity
public class AuthConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtFilter jwtFilter;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        super.configure(auth);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
//                .anonymous().and()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/anonymous-authenticator/id").permitAll()
                .antMatchers(HttpMethod.POST, "/token/**").permitAll()
                .antMatchers(HttpMethod.GET, "/actions/**/qr").permitAll()
                .antMatchers(HttpMethod.POST, "/actions/**/invocation").permitAll()
                .antMatchers(HttpMethod.GET, "/names").permitAll()
                .antMatchers(HttpMethod.GET, "/names/**").permitAll()
                .antMatchers(HttpMethod.POST, "/profile/delete-facebook-data-request").permitAll()
                .antMatchers(HttpMethod.GET, "/profile/delete-facebook-data-request/**").permitAll()
                .antMatchers("/admin/**").hasAuthority(ROLE_ADMIN)
                .anyRequest().hasAuthority(ROLE_USER).and()
                .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
