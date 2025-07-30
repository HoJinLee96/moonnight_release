package net.chamman.moonnight.global.security.principal;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;

@SuppressWarnings("serial")
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
	
	private final int userId;
	private final UserProvider userProvider; 
	private final String email; 
	private final String name; 
	private final List<GrantedAuthority> authorities; 
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public String getPassword() {
		return null; 
	}
	
	@Override
	public String getUsername() {
		return userId+""; 
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
	public CustomUserDetails(User user) {
		super();
		List<String> roles = List.of("ROLE_"+user.getUserProvider().name());
		List<GrantedAuthority> authorities =
				roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
				
		this.userId = user.getUserId();
		this.userProvider = user.getUserProvider();
		this.email = user.getEmail();
		this.name = user.getName();
		this.authorities = authorities;
	}


}
