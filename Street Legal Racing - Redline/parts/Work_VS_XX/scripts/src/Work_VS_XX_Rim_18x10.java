package java.game.parts.Work_VS_XX;

public class Work_VS_XX_Rim_18x10 extends Work_VS_XX_Rim
{
	public Work_VS_XX_Rim_18x10( int id )
	{
		RimRenders[ 0 ] = parts.Work_VS_XX:0x0000005Ar; // Black
		RimRenders[ 1 ] = parts.Work_VS_XX:0x0000005Br; // Blue
		RimRenders[ 2 ] = parts.Work_VS_XX:0x0000005Cr; // Brown
		RimRenders[ 3 ] = parts.Work_VS_XX:0x0000005Dr; // Gold
		RimRenders[ 4 ] = parts.Work_VS_XX:0x0000005Er; // Green
		RimRenders[ 5 ] = parts.Work_VS_XX:0x0000005Fr; // Metal
		RimRenders[ 6 ] = parts.Work_VS_XX:0x00000060r; // Red
		RimRenders[ 7 ] = parts.Work_VS_XX:0x00000061r; // White

		BoltsRenders[ 0 ] = parts.Work_VS_XX:0x00000036r; // Black
		BoltsRenders[ 1 ] = parts.Work_VS_XX:0x00000037r; // Blue
		BoltsRenders[ 2 ] = parts.Work_VS_XX:0x00000038r; // Gold
		BoltsRenders[ 3 ] = parts.Work_VS_XX:0x00000039r; // Green
		BoltsRenders[ 4 ] = parts.Work_VS_XX:0x0000003Ar; // Metal
		BoltsRenders[ 5 ] = parts.Work_VS_XX:0x0000003Br; // Red

		setupCustomRim( "Work VS-XX", 18.0, 10.0 );
	}
}
