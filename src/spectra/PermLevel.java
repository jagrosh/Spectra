/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

/**
 *
 * @author John Grosh (jagrosh)
 */
public enum PermLevel {
    EVERYONE(0), MODERATOR(1), ADMIN(2), JAGROSH(3);
    
    final int value;
    private PermLevel(int value)
    {
        this.value = value;
    }
    
    public boolean isAtLeast(PermLevel other)
    {
        return value >= other.value;
    }
}
