package net.chamman.moonnight.global.security.principal;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.chamman.moonnight.domain.admin.Admin;

@SuppressWarnings("serial")
@Getter
@AllArgsConstructor
public class CustomAdminDetails implements UserDetails {
	
	private final int adminId;
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
		return adminId+""; 
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
	
	public CustomAdminDetails(Admin admin) {
		super();
		List<String> roles = List.of("ROLE_ADMIN");
		List<GrantedAuthority> authorities =
				roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
				
		this.adminId = admin.getAdminId();
		this.email = admin.getEmail();
		this.name = admin.getName();
		this.authorities = authorities;
	}


}
