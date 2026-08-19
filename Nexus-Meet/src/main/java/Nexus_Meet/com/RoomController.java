package Nexus_Meet.com;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class RoomController {

    @GetMapping("/room")
    public String roomPage(Principal principal, @AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        String userName = "Guest User"; 
        
        // Agar Google OAuth Data mila
        if (oauth2User != null) {
            if (oauth2User.getAttribute("name") != null) {
                userName = oauth2User.getAttribute("name");
            } else if (oauth2User.getAttribute("email") != null) {
                String email = oauth2User.getAttribute("email");
                userName = email.substring(0, email.indexOf("@"));
            }
        } 
        // Fallback agar normal login hai
        else if (principal != null) {
            userName = principal.getName();
        }
        
        model.addAttribute("username", userName);
        return "room"; 
    }
}